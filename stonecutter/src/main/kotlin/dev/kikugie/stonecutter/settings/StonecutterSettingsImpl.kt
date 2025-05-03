package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.ProjectReference
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.StonecutterPlugin
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.createContainer
import dev.kikugie.stonecutter.data.tree.BranchBuilder
import dev.kikugie.stonecutter.data.tree.NodeBuilder
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import dev.kikugie.stonecutter.process.IdeaSetupTask
import dev.kikugie.stonecutter.util.*
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists

@OptIn(StonecutterInternalAPI::class)
internal open class StonecutterSettingsImpl @Inject constructor(private val settings: Settings, objects: ObjectFactory) :
    StonecutterSettingsExtension(objects) {
    final override val kotlinController: Property<Boolean> = objects.property()
    final override val centralScript: Property<String> = objects.property()
    internal val providers: ProviderFactory get() = settings.providers
    private var groovy = false
    private val container: TreeBuilderContainer = settings.gradle.createContainer()
    private val logger: Logger by logger("StonecutterSettings")

    init {
        logger.lifecycle { "Running Stonecutter ${StonecutterPlugin.VERSION}" }
        settings.gradle.settingsEvaluated {
            if (groovy) reportGroovyComplaint()
        }
        settings.gradle.projectsLoaded {
            createIdeaConfigurations(this, rootProject)
        }
        settings.gradle.createContainer<ProjectTreeContainer>()
    }

    override fun create(ref: ProjectReference, setup: TreeBuilder): Unit = with(setup) {
        val project = getDescriptor(ref)
        require(container.register(project.hierarchy, this)) { "Project ${project.path} is already registered" }

        val controller = controllerTypeFor()
        project.buildFileName = controller.filename.also(::checkGroovy)
        with(project.projectDir.resolve(controller.filename).toPath()) {
            if (notExists()) controller.create(this, vcsProject.project)
        }

        for (branch in branches.values) createBranch(project, branch)
    }

    internal fun getDefaultBuildScript(relative: String = "", type: String) =
        getDefaultBuildScript(settings.rootDir.toPath().let { if (relative.isEmpty()) it else it.resolve(relative) }, type)

    private fun getDefaultBuildScript(dir: Path, type: String): String = when {
        dir.resolve("$type.gradle.kts").exists() -> "$type.gradle.kts"
        dir.resolve("$type.gradle").exists() -> "$type.gradle"
        else -> "$type.gradle.kts"
    }

    private fun createBranch(root: ProjectDescriptor, branch: BranchBuilder) = with(branch) {
        require(nodes.isNotEmpty()) { "Registered branch '$id' has no nodes" }
        val project = if (id.isEmpty()) root else createDescriptor("${root.path}:$id")
        project.projectDir.toPath().createDirectories()
        if (id.isNotEmpty()) project.buildFileName = tree.controllerTypeFor(id).filename // TODO: to be tested
            .replace("stonecutter", "stonecutter_branch").also(::checkGroovy)

        for (node in nodes.values) createProject(project, node)
    }

    private fun createProject(root: ProjectDescriptor, node: NodeBuilder) = with(node) {
        val project = createDescriptor("${root.path}:${metadata.project}")
        val versionDir = File("${root.projectDir}/versions/${metadata.project}")
        versionDir.mkdirs()

        val script = buildscript.also(::checkGroovy)
        project.projectDir = versionDir
        project.name = metadata.project
        project.buildFileName = "../../$script"
    }

    private fun getDescriptor(ref: ProjectReference): ProjectDescriptor = when (ref) {
        is ProjectDescriptor -> ref
        is Provider<*> -> (ref.orNull as? String)?.let(::createDescriptor)
            ?: error("Project descriptor not found for $ref")
        is CharSequence -> createDescriptor(ref.toString())
        else -> error("Unsupported type ${ref::class.qualifiedName}")
    }

    private fun createDescriptor(path: String): ProjectDescriptor {
        val hierarchy = ProjectHierarchy.of(path)
        return if (hierarchy == ProjectHierarchy.ROOT)
            settings.rootProject
        else settings.run {
            include(hierarchy.toString().removePrefix(":"))
            project(hierarchy.toString())
        }
    }

    private fun createIdeaConfigurations(gradle: Gradle, root: Project) {
        root.tasks.register<IdeaSetupTask>("stonecutterIdea") {
            group = "ide"
        }

        if (isIdeaSync) gradle.requestTasks(listOf("stonecutterIdea"))
    }

    private fun checkGroovy(file: String) {
        if (groovy || file.endsWith(".gradle")) groovy = true
    }

    @Suppress("ReplacePrintlnWithLogging")
    private fun reportGroovyComplaint() = println("""
        NOTICE: Limited Groovy DSL support for Stonecutter
        
        While functional, the plugin's features are limited 
        by the Groovy syntax and it has reduced IDE support.
        For the best experience, including enhanced syntax, autocompletion, 
        documentation lookup and debugging, it's recommended to use Kotlin DSL.
        
        For more information see: 
          - https://stonecutter.codeberg.page/wiki/faq#groovy-support
    """.trimIndent())
}