package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.controller.manager.*
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.TreeBuilder
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.createContainer
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

// link: wiki-settings
/**
 * Configures versions used by Stonecutter and creates the corresponding Gradle projects.
 *
 * @see <a href="https://stonecutter.kikugie.dev/stonecutter/guide/setup#settings-settings-gradle-kts">Wiki page</a>
 */
@Suppress("MemberVisibilityCanBePrivate")
public open class StonecutterSettings(settings: Settings) : SettingsAbstraction(settings), StonecutterUtility {
    private companion object {
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
        println("Running Stonecutter $STONECUTTER") // Printed to help identify issues
        settings.gradle.settingsEvaluated {
            if (groovy) println(GROOVY_COMPLAINT_DOT_TXT)
        }
    }

    override fun create(project: ProjectDescriptor, setup: TreeBuilder) {
        require(container.register(project.path, setup)) { "Project ${project.path} is already registered" }
        if (!groovy && setup.isGroovyUsed()) groovy = true

        project.buildFileName = setup.controller.filename
        with(project.projectDir.resolve(setup.controller.filename).toPath()) {
            if (notExists()) setup.controller.createHeader(this, setup.vcsProject.project)
        }

        for ((name, branch) in setup.nodes) createBranch(name, project, setup, branch.values)
    }

    private fun TreeBuilder.isGroovyUsed() = !kotlinController
            || centralScript.endsWith(".gradle")
            || branches.values.any { it.buildscript.endsWith(".gradle") }

    private fun createBranch(name: Identifier, root: ProjectDescriptor, setup: TreeBuilder, branch: Collection<StonecutterProject>) {
        require(branch.isNotEmpty()) { "Registered branch $name has no nodes" }
        val project = if (name.isEmpty()) root else "${root.path}:$name".project()
        project.projectDir.toPath().createDirectories()
        project.buildFileName = setup.controller.filename

        val buildscript = checkNotNull(setup.branches[name]?.buildscript) { "Branch '$name' was not registered correctly" }
        for (it in branch) createProject(project, it, buildscript)
    }

    private fun createProject(root: ProjectDescriptor, version: StonecutterProject, buildscript: String) {
        val project = "${root.path}:${version.project}".project()
        val versionDir = File("${root.projectDir}/versions/${version.project}")
        versionDir.mkdirs()

        project.projectDir = versionDir
        project.name = version.project
        project.buildFileName = "../../$buildscript"
    }
}