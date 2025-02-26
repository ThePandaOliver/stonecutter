package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.data.tree.TreePrototype
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a project entry in a Stonecutter branch.
 */
@Serializable(with = StonecutterProject.StonecutterProjectSerializer::class)
public sealed interface StonecutterProject {
    /**The name of this project's directory, as in `versions/${project}`.*/
    @StonecutterAPI public val project: Identifier
    /**
     * The assigned version of this project. Can be either [SemanticVersion] or [AnyVersion].
     * By default, its equal to [project], unless assigned by using `vers()` in the project settings.
     */
    @StonecutterAPI public val version: AnyVersion

    @StonecutterAPI public val isActive: Boolean

    public operator fun component1(): Identifier = project
    public operator fun component2(): AnyVersion = version
    public operator fun component3(): Boolean = isActive

    @Serializable
    public data class DataStonecutterProject(
        override val version: AnyVersion,
        override val project: Identifier
    ) : StonecutterProject {
        @Transient
        override var isActive: Boolean = false
        override fun toString(): String = "$project:$version"
    }

    @Serializable(with = LinkedStonecutterProjectSerializer::class)
    public class LinkedStonecutterProject(
        internal val delegate: DataStonecutterProject
    ) : StonecutterProject by delegate {
        internal lateinit var tree: TreePrototype<*>
        override val isActive: Boolean get() = this == tree.current
        override fun toString(): String = "$project:$version"
    }

    private object StonecutterProjectSerializer : KSerializer<StonecutterProject> {
        private val serializer = DataStonecutterProject.serializer()
        override val descriptor: SerialDescriptor = serializer.descriptor

        override fun serialize(encoder: Encoder, value: StonecutterProject) = when(value) {
            is DataStonecutterProject -> serializer.serialize(encoder, value)
            is LinkedStonecutterProject -> serializer.serialize(encoder, value.delegate)
        }

        override fun deserialize(decoder: Decoder): StonecutterProject =
            serializer.deserialize(decoder)
    }

    private object LinkedStonecutterProjectSerializer : KSerializer<LinkedStonecutterProject> {
        private val serializer = DataStonecutterProject.serializer()
        override val descriptor: SerialDescriptor = serializer.descriptor
        override fun deserialize(decoder: Decoder): LinkedStonecutterProject {
            throw UnsupportedOperationException("LinkedStonecutterProject cannot be deserialized. Deserialize the base StonecutterProject instead")
        }

        override fun serialize(encoder: Encoder, value: LinkedStonecutterProject) =
            serializer.serialize(encoder, value.delegate)
    }

    public companion object {
        public fun create(project: Identifier, version: AnyVersion): StonecutterProject =
            DataStonecutterProject(version, project)
        public fun create(project: Identifier, version: AnyVersion, active: Boolean): StonecutterProject =
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
