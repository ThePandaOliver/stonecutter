package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable

@StonecutterAPI
public interface StonecutterBuildConfig : VersionOperations<ParsedVersion> {
    public val node: ProjectNode
    public val branch: ProjectBranch get() = node.branch
    public val tree: ProjectTree get() = branch.tree

    public val constants: ConstantContainer
    public val dependencies: DependencyContainer
    public val swaps: SwapContainer
    public val replacements: ReplacementContainer
    public val filters: PatternFilterable

    /**Provides [VersionOperations], which work strictly with [SemanticVersion]s.*/
    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    public fun constants(block: Action<ConstantContainer>): Unit = block.execute(constants)
    public fun dependencies(block: Action<DependencyContainer>): Unit = block.execute(dependencies)
    public fun swaps(block: Action<SwapContainer>): Unit = block.execute(swaps)
    public fun replacements(block: Action<ReplacementContainer>): Unit = block.execute(replacements)
    public fun filters(block: Action<PatternFilterable>): Unit = block.execute(filters)
}

