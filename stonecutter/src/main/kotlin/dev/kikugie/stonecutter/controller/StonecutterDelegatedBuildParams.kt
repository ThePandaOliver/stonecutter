package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode

public class StonecutterDelegatedBuildParams internal constructor (
    @StonecutterAPI public val node: ProjectNode,
    holder: StonecutterBuildData
) : StonecutterBuildParams by holder {
    @StonecutterAPI public val branch: ProjectBranch
        get() = node.branch
    @StonecutterAPI public val metadata: StonecutterProject
        get() = node.metadata
}