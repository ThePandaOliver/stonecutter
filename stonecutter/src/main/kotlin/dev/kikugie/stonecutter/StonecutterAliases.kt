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
 * which can be either [String], [Provider][org.gradle.api.provider.Provider] or
 * [ProjectDescriptor][org.gradle.api.initialization.ProjectDescriptor].
 */
@StonecutterAPI
public typealias ProjectReference = Any

public typealias TaskProviderMap<K, V> = Map<K, TaskProvider<V>>
public typealias TaskProviderMapProperty<K, V> = MapProperty<K, TaskProvider<V>>
internal typealias MutableTaskProviderMap<K, V> = MutableMap<K, TaskProvider<V>>