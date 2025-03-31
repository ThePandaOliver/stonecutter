package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.StonecutterBuildHolder
import dev.kikugie.stonecutter.build.StonecutterBuildParams
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode

public class StonecutterDelegatedBuildParams(
    @StonecutterAPI public val branch: ProjectBranch,
    @StonecutterAPI public val metadata: StonecutterProject,
    internal val holder: StonecutterBuildHolder
) : StonecutterBuildParams by holder {
    @StonecutterAPI public val node: ProjectNode?
        get() = branch[metadata.project]
}