package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.STONECUTTER
import dev.kikugie.stonecutter.StonecutterUtility
import dev.kikugie.stonecutter.controller.manager.GroovyController
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.createContainer
import dev.kikugie.stonecutter.data.tree.BranchBuilder
import dev.kikugie.stonecutter.data.tree.NodeBuilder
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import dev.kikugie.stonecutter.ide.IdeaSetupTask
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.DefaultTaskExecutionRequest
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists

// link: wiki-settings
/**
 * Configures versions used by Stonecutter and creates the corresponding Gradle projects.
 *
 * @see <a href="https://stonecutter.kikugie.dev/stonecutter/guide/setup#settings-settings-gradle-kts">Wiki page</a>
 */
@Suppress("MemberVisibilityCanBePrivate")
public abstract class StonecutterSettings @Inject constructor(settings: Settings, objects: ObjectFactory) :
    SettingsAbstraction(settings, objects),
    StonecutterUtility {
    internal companion object {
        const val DEFAULT_CONTROLLER_STATE = true
        const val DEFAULT_BUILD_SCRIPT = "build.gradle.kts"

        val GROOVY_COMPLAINT_DOT_TXT = """
            NOTICE: Limited Groovy DSL support for Stonecutter
            --------------------------------------------------------------------------------------------------------
            While functional, the plugin's features are limited by the Groovy syntax and it has reduced IDE support.
            
            For the best experience, including enhanced syntax, autocompletion, documentation lookup and debugging, 
            it's recommended to use Kotlin DSL.
            
            See for more info:
                https://stonecutter.kikugie.dev/stonecutter/guide/setup
                https://docs.gradle.org/current/userguide/migrating_from_groovy_to_kotlin_dsl.html
            --------------------------------------------------------------------------------------------------------
        """.trimIndent()
    }

    private var groovy = false
    private val container: TreeBuilderContainer = settings.gradle.createContainer()

    init {
        val factory = settings.providers
        val path = settings.rootDir.toPath()
        kotlinController.convention(factory.provider { getExistingBuildscript(path, "stonecutter").endsWith("kts") })
        centralScript.convention(factory.provider { getExistingBuildscript(path, "build")})

        println("Running Stonecutter $STONECUTTER") // Printed to help identify issues
        settings.gradle.settingsEvaluated {
            if (groovy) println(GROOVY_COMPLAINT_DOT_TXT)
        }
        settings.gradle.projectsLoaded {
            createIdeaConfigurations(this, rootProject)
        }
    }

    override fun create(project: ProjectDescriptor, setup: TreeBuilder): Unit = with(setup) {
        require(container.register(project.hierarchy, this)) { "Project ${project.path} is already registered" }

        val controller = controller.also {
            if (it == GroovyController) groovy = true
        }
        project.buildFileName = controller.filename
        with(project.projectDir.resolve(controller.filename).toPath()) {
            if (notExists()) controller.createHeader(this, vcsProject.project)
        }

        for (branch in branches.values) createBranch(project, branch)
    }

    private fun getExistingBuildscript(dir: Path, type: String): String = when {
        dir.resolve("$type.gradle.kts").exists() -> "$type.gradle.kts"
        dir.resolve("$type.gradle").exists() -> "$type.gradle"
        else -> "$type.gradle.kts"
    }

    private fun String.isGroovy() = endsWith(".gradle")

    private fun createBranch(root: ProjectDescriptor, branch: BranchBuilder) = with(branch) {
        require(nodes.isNotEmpty()) { "Registered branch '$id' has no nodes" }
        val project = if (id.isEmpty()) root else "${root.path}:$id".project()
        project.projectDir.toPath().createDirectories()
        project.buildFileName = tree.controller.filename
            .also { if (it.isGroovy()) groovy = true }

        for (node in nodes.values) createProject(project, node)
    }

    private fun createProject(root: ProjectDescriptor, node: NodeBuilder) = with(node) {
        val project = "${root.path}:${metadata.project}".project()
        val versionDir = File("${root.projectDir}/versions/${metadata.project}")
        versionDir.mkdirs()

        val script = buildscript.also { if (it.isGroovy()) groovy = true }
        project.projectDir = versionDir
        project.name = metadata.project
        project.buildFileName = "../../$script"
    }

    private fun createIdeaConfigurations(gradle: Gradle, root: Project) {
        root.tasks.register<IdeaSetupTask>("stonecutterIdea") {
            group = "ide"
        }
        if (System.getProperty("idea.sync.active", "false").toBoolean()) gradle.startParameter.run {
            if (taskRequests.none { "stonecutterIdea" in it.args })
                setTaskRequests(taskRequests + DefaultTaskExecutionRequest(listOf("stonecutterIdea")))
        }
    }
}