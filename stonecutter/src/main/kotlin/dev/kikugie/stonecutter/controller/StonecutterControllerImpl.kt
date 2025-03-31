package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.build.StonecutterBuildHolder
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.StonecutterProject.Companion.link
import dev.kikugie.stonecutter.data.StonecutterProject.Companion.linked
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.tree.*
import dev.kikugie.stonecutter.data.tree.withProject
import dev.kikugie.stonecutter.getChecked
import dev.kikugie.stonecutter.keysToString
import dev.kikugie.stonecutter.projectPath
import dev.kikugie.stonecutter.onEach
import dev.kikugie.stonecutter.process.FileMergingTask
import dev.kikugie.stonecutter.process.FileProcessingTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

internal open class StonecutterControllerImpl(private val root: Project) : StonecutterControllerExtension {
    override val tree: ProjectTree = constructTree().also {
        root.gradle.getContainer<ProjectTreeContainer>().register(root.hierarchy, it)
    }
    private val manager: StonecutterControllerManager get() = root.getController()!!
    private val configurations: MutableMap<ProjectHierarchy, StonecutterBuildHolder> = mutableMapOf()
    private var initialized = false

    init {
        configureProject()
    }

    override fun active(name: Identifier) {
        check(!initialized) { "Active version has already been set!" }
        tree.current = tree.getByName(name); initialized = true
    }

    override fun active(file: File) {
        check(!initialized) { "Active version has already been set!" }
        tree.provider = file; initialized = true
    }

    override fun parameters(config: StonecutterDelegatedBuildParams.() -> Unit) {
        for (branch in tree.branches) for (version in versions)
            getOrCreateParameters(branch.hierarchy + version.project)
                .let { StonecutterDelegatedBuildParams(branch, version, it).config() }
    }

    internal fun getOrCreateParameters(hierarchy: ProjectHierarchy) =
        configurations.getOrPut(hierarchy, ::StonecutterBuildHolder)

    private fun configureProject() = with(root) {
        afterEvaluate {
            if (plugins.hasPlugin("java")) logger.warn("Stonecutter branch root $hierarchy should not be a buildable project.")
        }
    }

    private fun updateController(version: StonecutterProject) = tree.provider
        ?.run { toPath().writeText(version.project, Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING) }
        ?: manager.update(root.buildFile.toPath(), version.project)

    private fun constructTree(): ProjectTree {
        val builder: TreeBuilder = checkNotNull(root.gradle.getContainer<TreeBuilderContainer>()[root]) {
            "Project ${root.path} is not registered. This might've been caused by removing a project while its active"
        }
        val mapping: Map<StonecutterProject, StonecutterProject> = builder.versions
            .mapValues { (_, it) -> it.linked() }
        val branches: Map<Identifier, LightBranch> = builder.branches.mapValues { (id, br) ->
            val project: Project = if (id.isEmpty()) root else root.project(id)
            val nodes: Map<Identifier, LightNode> = br.nodes.mapValues { (_, n) ->
                val identity = mapping.getChecked(n.metadata) { "Unknown version '$it' in ${keysToString()}"}
                LightNode(project.project(n.metadata.project).projectPath, identity)
            }
            LightBranch(project.projectPath, id, nodes).also {
                nodes.values.onEach { branch = it }
            }
        }
        val tree: LightTree = LightTree(root.projectPath, ProjectHierarchy(root.path), builder.vcsProject, branches).also {
            branches.values.onEach { tree = it }
            mapping.values.onEach { link(it) }
        }
        return tree.withProject(root)
    }
}