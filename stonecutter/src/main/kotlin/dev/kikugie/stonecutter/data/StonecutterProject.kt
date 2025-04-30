package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterDevAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.data.tree.TreePrototype
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a project entry in a Stonecutter branch.
 */
@Serializable(with = StonecutterProject.StonecutterProjectSerializer::class)
public sealed interface StonecutterProject {
    /**The name of this project's directory, as in `versions/${project}`.*/
    public val project: Identifier
    /**The assigned version of this project, used in comment evaluation.*/
    public val version: Version
    /**The active status of this version, transient in the serialization process.*/
    public val isActive: Boolean

    /**Returns the [project] component.*/
    public operator fun component1(): Identifier = project
    /**Returns the [version] component.*/
    public operator fun component2(): Version = version
    /**Returns the [isActive] component.*/
    public operator fun component3(): Boolean = isActive

    /**
     * Stonecutter project metadata with independent properties,
     * where [isActive] is mutable and **not serialised**.
     */
    @StonecutterInternalAPI @Serializable
    public data class DataStonecutterProject(
        override val version: Version,
        override val project: Identifier
    ) : StonecutterProject {
        /**Represents the active status with a mutable value defaulting to `false`*/
        @Transient override var isActive: Boolean = false
        /**Represents the project as '[project]:[version]'.*/
        override fun toString(): String = "$project:$version"
    }

    /**
     * Stonecutter project metadata that has its active status dynamically determined by the state of
     * [StonecutterController.current][dev.kikugie.stonecutter.controller.StonecutterControllerExtension.current].
     *
     * Since the stored tree holds a [Project][org.gradle.api.Project] reference,
     * it's unsafe to use with Gradle configuration cache.
     * Use [StonecutterProject.unlink] to get an independent entry.
     */
    @StonecutterInternalAPI @Serializable(with = LinkedStonecutterProjectSerializer::class)
    public class LinkedStonecutterProject(
        internal val delegate: DataStonecutterProject
    ) : StonecutterProject by delegate {
        /**Project tree initialised by [StonecutterController][dev.kikugie.stonecutter.controller.StonecutterControllerExtension].*/
        @Transient internal lateinit var tree: TreePrototype<*>
        /**
         * Represents the active status by comparing the current instance to
         * [StonecutterController.current][dev.kikugie.stonecutter.controller.StonecutterControllerExtension.current].
         */
        override val isActive: Boolean get() = this == tree.current
        /**Represents the project as '[project]:[version]'.*/
        override fun toString(): String = "$project:$version"
    }

    @OptIn(StonecutterInternalAPI::class)
    private object StonecutterProjectSerializer : KSerializer<StonecutterProject> {
        private val serializer = DataStonecutterProject.serializer()
        override val descriptor: SerialDescriptor = serializer.descriptor

        override fun serialize(encoder: Encoder, value: StonecutterProject): Unit = when(value) {
            is DataStonecutterProject -> serializer.serialize(encoder, value)
            is LinkedStonecutterProject -> serializer.serialize(encoder, value.delegate)
        }

        override fun deserialize(decoder: Decoder): StonecutterProject =
            serializer.deserialize(decoder)
    }

    @OptIn(StonecutterInternalAPI::class)
    private object LinkedStonecutterProjectSerializer : KSerializer<LinkedStonecutterProject> {
        private val serializer = DataStonecutterProject.serializer()
        override val descriptor: SerialDescriptor = serializer.descriptor
        override fun deserialize(decoder: Decoder): LinkedStonecutterProject {
            throw UnsupportedOperationException("LinkedStonecutterProject cannot be deserialized. Deserialize the base StonecutterProject instead")
        }

        override fun serialize(encoder: Encoder, value: LinkedStonecutterProject) =
            serializer.serialize(encoder, value.delegate)
    }

    @OptIn(StonecutterInternalAPI::class)
    public companion object {
        /**
         * Returns the matching [DataStonecutterProject],
         * which may not have the instance identity to the original one.
         */
        public fun unlink(project: StonecutterProject): StonecutterProject = when(project) {
            is DataStonecutterProject -> project
            is LinkedStonecutterProject -> DataStonecutterProject(project.version, project.project)
                .apply { isActive = project.isActive }
        }

        /**
         * Creates a new [DataStonecutterProject] instance.
         * - **During project configuration Stonecutter expects registered versions to have instance identity.
         * Creating new instances may lead to undefined behaviour.**
         */
        @StonecutterInternalAPI
        public fun create(project: Identifier, version: Version): StonecutterProject =
            DataStonecutterProject(version, project)

        /**
         * Creates a new [DataStonecutterProject] instance.
         * - **During project configuration Stonecutter expects registered versions to have instance identity.
         * Creating new instances may lead to undefined behaviour.**
         */
        @StonecutterInternalAPI
        public fun create(project: Identifier, version: Version, active: Boolean): StonecutterProject =
            DataStonecutterProject(version, project).apply { isActive = active }

        internal fun StonecutterProject.linked(): StonecutterProject = when(this) {
            is DataStonecutterProject -> LinkedStonecutterProject(this)
            is LinkedStonecutterProject -> this
        }

        internal fun StonecutterProject.link(tree: TreePrototype<*>): Unit = linked().let {
            (it as LinkedStonecutterProject).tree = tree
        }
    }
}
