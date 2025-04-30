package dev.kikugie.stonecutter

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension
import dev.kikugie.stonecutter.settings.StonecutterSettingsImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware

public open class StonecutterPlugin : Plugin<ExtensionAware> {
    public companion object {
        /**Current Stonecutter version.*/ // Updated by ':updateVersion' task during build
        public const val VERSION: String = "0.7-alpha.11"
    }

    /**
     * Applies the plugin either to [Settings] or [Project].
     * Applying the plugin to an incorrect target will throw an exception.
     */
    override fun apply(target: ExtensionAware): Unit = target.applyPlugin()

    @OptIn(StonecutterInternalAPI::class)
    private fun ExtensionAware.applyPlugin() = when (this) {
        is Settings ->
            stonecutter<StonecutterSettingsExtension, StonecutterSettingsImpl>()
        is Project ->
            if (getController() == null) stonecutter<StonecutterBuildExtension, StonecutterBuildImpl>()
            else stonecutter<StonecutterControllerExtension, StonecutterControllerImpl>()
        else ->
            error("The plugin may only be applied to settings and projects")
    }

    private inline fun <reified P, reified R : P> ExtensionAware.stonecutter() {
        extensions.create(P::class.java, "stonecutter", R::class.java, this)
    }
}