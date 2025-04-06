package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree

public interface StonecutterBuildExtension : StonecutterBuildParams {
    @StonecutterAPI public val tree: ProjectTree
    @StonecutterAPI public val branch: ProjectBranch
    @StonecutterAPI public val node: ProjectNode
    @StonecutterAPI public val flags: FlagContainer

    @StonecutterAPI public val active: StonecutterProject
        get() = tree.current
    @StonecutterAPI public val current: StonecutterProject
        get() = node.metadata
    @StonecutterAPI public val versions: Collection<StonecutterProject>
        get() = branch.versions
}