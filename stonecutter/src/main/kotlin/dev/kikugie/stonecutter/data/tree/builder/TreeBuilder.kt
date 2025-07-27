package dev.kikugie.stonecutter.data.tree.builder

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.data.StonecutterProject
import groovy.lang.Closure
import org.gradle.api.provider.Property

public abstract class TreeBuilder : BranchBuilder() {
    public abstract val vcsVersion: Property<String>
    public abstract val centralScript: Property<String>
    public abstract val kotlinController: Property<Boolean>

    public fun branch(name: Identifier): Unit =
        branch(name) { inherit() }

    public fun branch(name: Identifier, closure: Closure<BranchBuilder>): Unit =
        branch(name, closure::call)
    
    public abstract fun branch(name: Identifier, action: BranchBuilder.() -> Unit)
    public abstract fun mapBuilds(action: (Identifier, StonecutterProject) -> String)
}

public abstract class BranchBuilder {
    public abstract val branchScript: Property<String>

    @Deprecated("Use the fully spelled version", replaceWith = ReplaceWith("version(name, identifier)"))
    public fun vers(name: Identifier, version: Version): NodeBuilder =
        version(name, version)

    public fun version(version: Version): NodeBuilder =
        versions(listOf(StonecutterProject(version, version)))

    public fun version(name: Identifier, version: Version): NodeBuilder =
        versions(listOf(StonecutterProject(name, version)))

    public fun versions(versions: Map<Identifier, Version>): NodeBuilder =
        versions(versions.map { (k, v) -> StonecutterProject(k, v) })

    public fun versions(vararg versions: Identifier): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    public fun versions(versions: Iterable<Identifier>): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    @JvmName("versionPairs")
    public fun versions(vararg versions: Pair<Identifier, Version>): NodeBuilder =
        versions(versions.map { (p, v) -> StonecutterProject(p, v) })

    @JvmName("versionPairs")
    public fun versions(versions: Iterable<Pair<Identifier, Version>>): NodeBuilder =
        versions(versions.map { (p, v) -> StonecutterProject(p, v) })

    public abstract fun inherit()
    protected abstract fun versions(versions: List<StonecutterProject>): NodeBuilder
}

public abstract class NodeBuilder {
    public abstract val buildscript: Property<String>

    public fun buildscript(name: String): NodeBuilder =
        apply { buildscript.set(name) }
}