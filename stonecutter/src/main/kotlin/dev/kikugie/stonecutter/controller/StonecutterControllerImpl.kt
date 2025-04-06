package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasksImpl
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.StonecutterProject.Companion.link
import dev.kikugie.stonecutter.data.StonecutterProject.Companion.linked
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.tree.*
import dev.kikugie.stonecutter.getChecked
import dev.kikugie.stonecutter.keysToString
import dev.kikugie.stonecutter.onEach
import org.gradle.api.Project
import org.gradle.kotlin.dsl.newInstance
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

internal open class StonecutterControllerImpl(private val root: Project) : StonecutterControllerExtension {
    override val tree: ProjectTree = constructTree().also {
        root.gradle.getContainer<ProjectTreeContainer>().register(root.hierarchy, it)
    }
    private val manager: StonecutterControllerManager get() = root.getController()!!
    private val configurations: MutableMap<ProjectHierarchy, StonecutterBuildData> = mutableMapOf()
    private var initilizer: (() -> Unit)? = ::initializeSwitchTasks
    internal val tasksImpl = StonecutterControllerTasksImpl()

    init {
        configureProject()
    }

    override fun active(name: Identifier) {
        checkNotNull(initilizer) { "Active version has already been set!" }
        tree.current = tree.getByName(name); initilizer!!(); initilizer = null
    }

    override fun active(file: File) {
        checkNotNull(initilizer) { "Active version has already been set!" }
        tree.provider = file; initilizer!!(); initilizer = null
    }

    override fun parameters(config: StonecutterDelegatedBuildParams.() -> Unit) {
        for (branch in tree.branches) for (version in versions)
            getOrCreateParameters(branch.hierarchy + version.project)
                .let { StonecutterDelegatedBuildParams(branch, version, it).config() }
    }

    internal fun getOrCreateParameters(hierarchy: ProjectHierarchy): StonecutterBuildData =
        configurations.getOrPut(hierarchy) { root.objects.newInstance() }

    private fun configureProject() = with(root) {
        afterEvaluate {
            if (plugins.hasPlugin("java")) logger.warn("Stonecutter branch root $hierarchy should not be a buildable project.")
        }

        tasks.register("Reset active project") {
            group = "stonecutter"
            description = "Sets active version to ${tree.vcs.project}. Run this before making a commit."
            dependsOn("${root.hierarchy.orBlank()}:${tasksImpl.switchTaskName(tree.vcs.project)}")
        }

        tasks.register("Refresh active project") {
            group = "stonecutter"
            description = "Runs the comment processor on the active version. Useful for fixing comments in wrong states."
            dependsOn("${root.hierarchy.orBlank()}:${tasksImpl.switchTaskName(tree.current.project)}")
        }

        for (it in tree.versions) tasks.register("Set active project to ${it.project}") {
            group = "stonecutter"
            description = "Sets the active project to ${it.project}, processing all versioned comments."
            dependsOn("${root.hierarchy.orBlank()}:${tasksImpl.switchTaskName(it.project)}")
        }
    }

    private fun initializeSwitchTasks() {
        for (it in tree.versions) tasksImpl.registerSwitchTask(it.project, tree, manager)
    }

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
                LightNode(project.project(n.metadata.project).projectDir.toPath(), identity)
            }
            LightBranch(project.projectDir.toPath(), id, nodes).also {
                nodes.values.onEach { branch = it }
            }
        }
        val tree: LightTree = LightTree(root.projectDir.toPath(), ProjectHierarchy(root.path), builder.vcsProject, branches).also {
            branches.values.onEach { tree = it }
            mapping.values.onEach { link(it) }
        }
        return tree.withProject(root)
    }
}