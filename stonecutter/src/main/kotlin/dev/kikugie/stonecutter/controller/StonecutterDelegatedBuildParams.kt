package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildParams
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode

public class StonecutterDelegatedBuildParams internal constructor (
    public val node: ProjectNode,
    holder: StonecutterBuildData
) : StonecutterBuildParams by holder {
    public val branch: ProjectBranch get() = node.branch
    public val metadata: StonecutterProject get() = node.metadata
}