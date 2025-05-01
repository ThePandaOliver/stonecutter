package dev.kikugie.stonecutter.data.tree.struct

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import org.gradle.api.Project
import java.nio.file.Path

public interface GradleMember {
    public val location: Path
    public val hierarchy: ProjectHierarchy

    @get:Throws
    public val project: Project
}

public interface ProjectNode : GradleMember {
    public val metadata: StonecutterProject
    public val branch: ProjectBranch

    public fun peer(node: Identifier): ProjectNode? = find(branch.id, node)
    public fun sibling(branch: Identifier): ProjectNode? = find(branch, metadata.project)
    public fun find(branch: Identifier, node: Identifier): ProjectNode? = this.branch.tree.get(branch)?.get(node)
}

public interface ProjectBranch : GradleMember, Map<Identifier, ProjectNode> {
    public val id: Identifier
    public val tree: ProjectTree
    public val nodes: Collection<ProjectNode>
    public val versions: Collection<StonecutterProject>

    public operator fun get(node: ProjectHierarchy): ProjectNode?
}

public interface ProjectTree : GradleMember, Map<Identifier, ProjectBranch> {
    public val vcs: StonecutterProject
    public val current: StonecutterProject?
    public val branches: Collection<ProjectBranch>
    public val nodes: Collection<ProjectNode>
    public val versions: Collection<StonecutterProject>

    public fun node(node: ProjectHierarchy): ProjectNode?
    public operator fun get(branch: ProjectHierarchy): ProjectBranch?
}