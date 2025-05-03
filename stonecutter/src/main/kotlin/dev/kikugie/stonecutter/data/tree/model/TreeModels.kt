@file:UseSerializers(PathSerializer::class, FlagContainerJsonSerializer::class)
package dev.kikugie.stonecutter.data.tree.model

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.data.StonecutterProject
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Path

@Serializable
public data class NodeInfo(
    val project: Identifier,
    val version: Version = project,
    val active: Boolean = false,
    val path: Path
) {
    public constructor(metadata: StonecutterProject, path: Path)
        : this(metadata.project, metadata.version, metadata.isActive, path)
}

@Serializable
public data class BranchInfo(
    val id: String,
    val path: Path
)

@Serializable
public data class NodeModel(
    val project: Identifier,
    val version: Version = project,
    val active: Boolean = false,
    val branch: BranchInfo,
    val root: Path,
    val parameters: StonecutterBuildData,
) {
    public constructor(metadata: StonecutterProject, branch: BranchInfo, root: Path, parameters: StonecutterBuildData)
        : this(metadata.project, metadata.version, metadata.isActive, branch, root, parameters)
}

@Serializable
public data class BranchModel(
    val id: String,
    val root: Path,
    val nodes: List<NodeInfo>,
)

@Serializable
public data class TreeModel(
    val stonecutter: String,
    val vcs: Identifier,
    val current: Identifier? = null,
    val branches: List<BranchInfo>,
    val nodes: List<NodeInfo>,
    val flags: FlagContainer,
)