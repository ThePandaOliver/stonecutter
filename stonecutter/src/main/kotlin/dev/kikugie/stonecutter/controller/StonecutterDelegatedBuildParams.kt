package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.build.param.DeprecatedBuildParams
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode

public class StonecutterDelegatedBuildParams internal constructor (
    public val node: ProjectNode,
    holder: StonecutterBuildProperties
) : DeprecatedBuildParams by holder {
    public val branch: ProjectBranch get() = node.branch
    public val metadata: StonecutterProject get() = node.metadata
}