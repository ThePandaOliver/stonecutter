package dev.kikugie.stonecutter.controller

import dev.kikugie.semver.data.Version as ParsedVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.StonecutterPlugin
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.controller.flag.FlagContainerImpl
import dev.kikugie.stonecutter.controller.flag.MutableFlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasksImpl
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.container.ProjectTreeContainer
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.*
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranchImpl
import dev.kikugie.stonecutter.data.tree.struct.ProjectNodeImpl
import dev.kikugie.stonecutter.data.tree.struct.ProjectTreeImpl
import dev.kikugie.stonecutter.process.IdeaSetupTask
import dev.kikugie.stonecutter.util.newInstance
import dev.kikugie.stonecutter.util.requestTasks
import dev.kikugie.stonecutter.util.set
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.named
import java.io.File

@OptIn(StonecutterInternalAPI::class)
internal open class StonecutterControllerImpl(val root: Project) : StonecutterControllerExtension,
    VersionOperations<ParsedVersion> by LenientOperations {
    override val tree: ProjectTreeImpl = constructTree().also {
        root.gradle.getContainer<ProjectTreeContainer>().register(root.hierarchy, it)
    }
    override val flags: MutableFlagContainer = FlagContainerImpl()
    override val tasks: StonecutterControllerTasksImpl = StonecutterControllerTasksImpl(this)

    /**Stores configured build data instances.*/
    private val data: MutableMap<ProjectHierarchy, StonecutterBuildProperties> = mutableMapOf()

    /**
     * Stores lazy functions for build configuration.
     * When a [StonecutterBuildData] instance is created,
     * the functions are applied and removed from the map.
     */
    private val configurations: MutableMap<ProjectHierarchy, MutableList<(StonecutterDelegatedBuildParams) -> Unit>> = mutableMapOf()

    init {
        configureProject()
        configureSyncTask()
        configureModelTasks()
    }

    override fun active(name: Identifier) {
        check(tree.current == null) { "Active version has already been set!" }
        initializePluginConfiguration(name)
    }

    override fun active(file: File) {
        check(tree.current == null) { "Active version has already been set!" }
        initializePluginConfiguration(file)
    }

    override fun parameters(config: StonecutterDelegatedBuildParams.() -> Unit) {
        for (branch in tree.branches) for (version in versions) configurations
            .getOrPut(branch.hierarchy + version.project, ::mutableListOf) += config
    }

    internal fun getOrCreateParameters(hierarchy: ProjectHierarchy): StonecutterBuildProperties = data.getOrPut(hierarchy) {
        root.objects.newInstance {
            if (hierarchy !in this@StonecutterControllerImpl.configurations) return@newInstance
            val delegate = StonecutterDelegatedBuildParams(this@StonecutterControllerImpl.tree.nodes.first { it.hierarchy == hierarchy }, this)
            for (config in this@StonecutterControllerImpl.configurations.remove(hierarchy) ?: emptyList()) config(delegate)
        }
    }

    private fun configureProject() = with(root) {
        afterEvaluate {
            if (plugins.hasPlugin("java")) logger.warn("Stonecutter branch root $hierarchy should not be a buildable project.")
        }

        if (tree.current == null) return@with
        tasks.register("Reset active project") {
            group = "stonecutter"
            description = "Sets active version to ${tree.vcs.project}. Run this before making a commit."
            dependsOn("${root.hierarchy.orBlank()}:${this@StonecutterControllerImpl.tasks.switchTaskName(tree.vcs.project)}")
        }

        tasks.register("Refresh active project") {
            group = "stonecutter"
            description = "Runs the comment processor on the active version. Useful for fixing comments in wrong states."
            dependsOn("${root.hierarchy.orBlank()}:${this@StonecutterControllerImpl.tasks.switchTaskName(tree.current!!.project)}")
        }

        for (it in tree.versions) tasks.register("Set active project to ${it.project}") {
            group = "stonecutter"
            description = "Sets the active project to ${it.project}, processing all versioned comments."
            dependsOn("${root.hierarchy.orBlank()}:${this@StonecutterControllerImpl.tasks.switchTaskName(it.project)}")
        }
    }

    private fun initializePluginConfiguration(active: Any) {
        fun findByName(name: String): StonecutterProject = checkNotNull(tree.versions.find { it.project == name }) {
            "Version '$name' is not registered. This might've been caused by removing a version that is set to be active."
        }

        when (active) {
            is String -> {
                tree.current = findByName(active)
                val controller = root.getController()!!
                for (it in tree.versions) tasks.registerSelfSwitchTask(it.project, controller)
            }

            is File -> {
                tree.current = findByName(active.readText())
                for (it in tree.versions) tasks.registerExternalSwitchTask(it.project, active)
            }
        }

        if (flags[StonecutterFlag.APPLY_PLUGIN_TO_NODES]) for (it in tree.nodes)
            it.project.plugins.apply(StonecutterPlugin::class)
    }

    private fun configureSyncTask() = root.afterEvaluate {
        if (flags[StonecutterFlag.GENERATE_SWITCH_ACTIONS]) rootProject.tasks.named<IdeaSetupTask>("stonecutterIdea") {
            versions[tree.hierarchy] = tree.versions.map(StonecutterProject::project)
        }
    }

    private fun configureModelTasks() = with(tasks) {
        registerModelGroupingTask()
        registerTreeModelTask()
        for (branch in tree.branches) registerBranchModelTask(branch)
        root.gradle.requestTasks(listOf("stonecutterSaveModels"))
    }

    private fun constructTree(): ProjectTreeImpl {
        val builder = checkNotNull(root.gradle.getContainer<TreeBuilderContainer>()[root]) {
            "Project ${root.path} is not registered. This might've been caused by removing a project while its active"
        }
        val branches = builder.constructBranches(root.hierarchy)
        return ProjectTreeImpl(root.gradle, root.hierarchy, builder.vcsProject, branches).apply {
            for (it in branches) it.tree = this
        }
    }

    private fun TreeBuilder.constructBranches(tree: ProjectHierarchy) = branches.values.map {
        val nodes = it.constructNodes(tree + it.id)
        ProjectBranchImpl(root.gradle, tree + it.id, it.id, nodes).apply {
            for (node in nodes) node.branch = this
        }
    }

    private fun BranchBuilder.constructNodes(branch: ProjectHierarchy) = nodes.values.map {
        ProjectNodeImpl(root.gradle, branch + it.metadata.project, it.metadata)
    }
}