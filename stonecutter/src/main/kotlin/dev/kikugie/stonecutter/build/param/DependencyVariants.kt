package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.container.ParameterMap

public interface DependencyVariants {
    @StonecutterAPI
    public val dependencies: ParameterMap<Identifier, AnyVersion>

    @StonecutterAPI
    public fun dependency(id: Identifier, version: AnyVersion) {
        dependencies[id] = version
    }

    @StonecutterAPI
    public fun dependency(id: Identifier, version: () -> AnyVersion) {
        dependencies[id] = version()
    }

    @StonecutterAPI
    public fun dependencies(vararg values: Pair<Identifier, AnyVersion>) {
        dependencies.putAll(values)
    }

    @StonecutterAPI
    public fun dependencies(values: Iterable<Pair<Identifier, AnyVersion>>) {
        dependencies.putAll(values)
    }

    @StonecutterAPI
    public fun dependencies(values: Map<Identifier, AnyVersion>) {
        dependencies.putAll(values)
    }
}