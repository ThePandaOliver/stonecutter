package dev.kikugie.stonecutter

import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.TaskProvider

/**
 * Constrained string used in project and parameter names.
 * Parameters of this type are usually checked to satisfy the following requirements:
 * 1. Must not be empty.
 * 2. The first character must match `[_a-zA-Z]`.
 * 3. The following characters must match `[_-+\.a-zA-Z0-9]`.
 *
 * @see dev.kikugie.stonecutter.util.isIdentifier
 */
@StonecutterAPI
public typealias Identifier = String

@StonecutterAPI
public typealias Version = String

/**
 * Project notation used in [StonecutterSettings][dev.kikugie.stonecutter.settings.StonecutterSettingsExtension],
 * Allowed types are:
 * - A [String] representing the project path. The `:` prefix can be omitted.
 * - An existing [ProjectDescriptor][org.gradle.api.initialization.ProjectDescriptor].
 * - A [Provider][org.gradle.api.provider.Provider] of the types above.
 */
@StonecutterAPI
public typealias ProjectReference = Any

/**
 * Dynamically resolved active project reference.
 * Allowed types are:
 * - A **literal** [String] passed directly to the function.
 * Using it will cause the `stonecutter.gradle[.kts] file to be updated after a switch.
 * - [File][java.io.File], [RegularFile][org.gradle.api.file.RegularFile]
 * or [Provider][org.gradle.api.provider.Provider] of these types.
 * The file must be in ASCII or UTF-8 encoding containing **only** the active project name
 * (a trailing newline is allowed), which will be updated after a switch.
 * - `null` is used to initialize Stonecutter in detached mode (without an active version).
 * It is useful for CI builds but has to be used with caution.
 */
@StonecutterAPI
public typealias ActiveReference = Any?

public typealias TaskProviderMap<K, V> = Map<K, TaskProvider<V>>
public typealias TaskProviderMapProperty<K, V> = MapProperty<K, TaskProvider<V>>
internal typealias MutableTaskProviderMap<K, V> = MutableMap<K, TaskProvider<V>>