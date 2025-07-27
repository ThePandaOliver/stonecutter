package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.ProjectReference
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.StonecutterPlugin
import dev.kikugie.stonecutter.data.container.BuildPropertiesContainer
import dev.kikugie.stonecutter.data.container.ProjectNodeContainer
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.createContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.builder.TreeBuilder
import dev.kikugie.stonecutter.data.tree.builder.TreeBuilderImpl
import dev.kikugie.stonecutter.process.SCIdeaConfigTask
import dev.kikugie.stonecutter.util.isIdeaSync
import dev.kikugie.stonecutter.util.lifecycle
import dev.kikugie.stonecutter.util.logger
import dev.kikugie.stonecutter.util.requestTasks
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import javax.inject.Inject
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class)
internal open class StonecutterSettingsImpl @Inject constructor(internal val settings: Settings, objects: ObjectFactory) :
    StonecutterSettingsExtension(objects), VersionOperations<ParsedVersion> by LenientOperations {
    final override val kotlinController: Property<Boolean> = objects.property()
    final override val centralScript: Property<String> = objects.property()
    internal val container: TreeBuilderContainer = settings.gradle.createContainer()
    internal val isHardMode: Boolean by lazy {
        settings.providers.gradleProperty("dev.kikugie.stonecutter.hard_mode").getOrElse("false").toBoolean()
    }

    private val logger: Logger by logger("StonecutterSettings")
    private var groovy: Boolean = false

    init {
        logger.lifecycle { "Running Stonecutter ${StonecutterPlugin.VERSION}" }
        settings.gradle.createContainer<ProjectNodeContainer>()
        settings.gradle.createContainer<BuildPropertiesContainer>(objects, settings.providers)
        settings.gradle.settingsEvaluated {
            if (groovy && !isHardMode) reportGroovyComplaint()
        }
        settings.gradle.projectsLoaded {
            createIdeaConfigurations(this, rootProject)
        }
    }

    override fun create(ref: ProjectReference, builder: TreeBuilder) {
        (builder as TreeBuilderImpl).createWith(this, ref)
    }

    internal fun checkGroovy(file: String): String = file.also {
        if (it.endsWith(".gradle")) groovy = true
    }

    private fun createIdeaConfigurations(gradle: Gradle, root: Project) {
        root.tasks.register<SCIdeaConfigTask>("stonecutterIdea") {
            group = "ide"
        }

        if (isIdeaSync) gradle.requestTasks(listOf("stonecutterIdea"), root.path, root.projectDir)
    }

    private fun reportGroovyComplaint() = logger.warn("""
        NOTICE: Limited Groovy DSL support for Stonecutter
        
        While functional, the plugin's features are limited 
        by the Groovy syntax and it has reduced IDE support.
        For the best experience, including enhanced syntax, autocompletion, 
        documentation lookup and debugging, it's recommended to use Kotlin DSL.
        
        For more information see: 
          - https://stonecutter.codeberg.page/wiki/faq#groovy-support
        """.trimIndent())
}