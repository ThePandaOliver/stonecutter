package dev.kikugie.stonecutter

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

/**
 * Project notation used in [StonecutterSettings][dev.kikugie.stonecutter.settings.StonecutterSettingsExtension],
 * which can be either [CharSequence], [Provider<String>][org.gradle.api.provider.Provider] or
 * [ProjectDescriptor][org.gradle.api.initialization.ProjectDescriptor].
 */
@StonecutterAPI
public typealias ProjectReference = Any