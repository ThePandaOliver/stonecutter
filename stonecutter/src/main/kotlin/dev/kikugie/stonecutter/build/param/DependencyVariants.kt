package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI

public interface DependencyVariants {
    @StonecutterAPI
    public val dependencies: MutableMap<Identifier, Version>

    @StonecutterAPI
    public fun dependency(id: Identifier, version: Version) {
        dependencies[id] = version
    }

    @StonecutterAPI
    public fun dependency(id: Identifier, version: () -> Version) {
        dependencies[id] = version()
    }

    @StonecutterAPI
    public fun dependencies(vararg values: Pair<Identifier, Version>) {
        dependencies.putAll(values)
    }

    @StonecutterAPI
    public fun dependencies(values: Iterable<Pair<Identifier, Version>>) {
        dependencies.putAll(values)
    }

    @StonecutterAPI
    public fun dependencies(values: Map<Identifier, Version>) {
        dependencies.putAll(values)
    }
}