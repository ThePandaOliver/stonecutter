package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.controller.manager.GroovyController
import dev.kikugie.stonecutter.controller.manager.KotlinController
import dev.kikugie.stonecutter.data.container.ProjectParameterContainer
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.settings.builder.TreeBuilder
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.create
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
open class StonecutterSettings(settings: Settings) : SettingsConfiguration(settings), StonecutterUtility {
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
    private val container: TreeBuilderContainer
    private val controller get() = if (kotlinController) KotlinController else GroovyController

    /**
     * Enables Kotlin buildscripts for the controller.
     * - `stonecutter.gradle` -> `stonecutter.gradle.kts`
     */
    var kotlinController: Boolean = false

    /**Buildscript used by all subprojects. Defaults to `build.gradle`.*/
    var centralScript: String = "build.gradle"
        set(value) {
            require(!value.startsWith("stonecutter.gradle")) {
                "Build script must not override the controller"
            }
            field = value
        }

    init {
        println("Running Stonecutter $STONECUTTER") // Printed to help identify issues
        with(settings.gradle.extensions) {
            container = create<TreeBuilderContainer>("stonecutterTreeBuilders")
            create<ProjectTreeContainer>("stonecutterProjectTrees")
            create<ProjectParameterContainer>("stonecutterProjectParameters")
        }
        settings.gradle.settingsEvaluated {
            if (groovy) println(GROOVY_COMPLAINT_DOT_TXT)
        }
    }

    override fun create(project: ProjectDescriptor, setup: TreeBuilder) {
        require(container.register(project.path, setup)) { "Project ${project.path} is already registered" }
        groovy = groovy || isGroovyUsed(setup)

        project.buildFileName = controller.filename
        with(project.projectDir.resolve(controller.filename).toPath()) {
            if (notExists()) controller.createHeader(this, setup.vcsVersion!!)
        }

        setup.nodes.forEach { (name, branch) ->
            createBranch(name, project, setup, branch)
        }
    }

    private fun isGroovyUsed(setup: TreeBuilder) = !kotlinController
            || centralScript.endsWith(".gradle")
            || setup.branches.values.any { it.buildscript.endsWith(".gradle") }

    private fun createBranch(
        name: Identifier,
        root: ProjectDescriptor,
        setup: TreeBuilder,
        branch: Collection<StonecutterProject>
    ) {
        require(branch.isNotEmpty()) { "Registered branch $name has no nodes" }
        val project = if (name.isEmpty()) root else "${root.path}:$name".project()
        project.projectDir.toPath().createDirectories()
        project.buildFileName = controller.filename

        val buildscript = runCatching { setup.branches[name]!!.buildscript }.getOrElse { centralScript }
        branch.forEach { createProject(project, it, buildscript) }
    }

    private fun createProject(
        root: ProjectDescriptor,
        version: StonecutterProject,
        buildscript: String
    ) {
        val project = "${root.path}:${version.project}".project()
        val versionDir = File("${root.projectDir}/versions/${version.project}")
        versionDir.mkdirs()

        project.projectDir = versionDir
        project.name = version.project
        project.buildFileName = "../../$buildscript"
    }
}