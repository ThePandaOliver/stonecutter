package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterUtility
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.controller.flag.MutableFlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasks
import groovy.lang.Closure
import java.io.File

public interface StonecutterControllerExtension : StonecutterUtility {
    @StonecutterAPI public val tree: ProjectTree
    @StonecutterAPI public val current: StonecutterProject get() = tree.current
    @StonecutterAPI public val vcsVersion: StonecutterProject get() = tree.vcs
    @StonecutterAPI public val versions: Collection<StonecutterProject> get() = tree.versions

    @StonecutterAPI public val flags: MutableFlagContainer
    @StonecutterAPI public infix fun <T : Any> StonecutterFlag<T>.assign(value: T): Unit =
        flags.set(this, value)
    public val tasks: StonecutterControllerTasks

    @StonecutterAPI public infix fun active(name: Identifier)
    @StonecutterAPI public infix fun active(file: File)

    @StonecutterAPI public infix fun parameters(config: StonecutterDelegatedBuildParams.() -> Unit)
    @StonecutterAPI public fun parameters(config: Closure<StonecutterDelegatedBuildParams>): Unit =
        parameters(config::call)
}