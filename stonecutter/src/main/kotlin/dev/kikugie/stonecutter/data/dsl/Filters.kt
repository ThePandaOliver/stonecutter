package dev.kikugie.stonecutter.data.dsl

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.commons.then
import org.gradle.api.Action

@StonecutterAPI @StonecutterParametersDSL
public interface FilterContainer {
    @StonecutterFilterSpec
    public interface AllowedExtensions : MutableSet<String> {
        public fun allow(vararg extensions: String): Boolean = addAll(extensions)
        public fun allow(extensions: Iterable<String>): Boolean = addAll(extensions)

        public fun disallow(vararg extensions: String): Boolean = removeAll(extensions)
        public fun disallow(extensions: Iterable<String>): Boolean = removeAll(extensions)

        public fun replace(vararg extensions: String): Boolean = clear() then allow(*extensions)
        public fun replace(extensions: Iterable<String>): Boolean = clear() then allow(extensions)
    }

    @StonecutterFilterSpec
    public interface ExcludedFiles : MutableSet<String> {
        public fun exclude(vararg files: String): Boolean = addAll(files)
        public fun exclude(files: Iterable<String>): Boolean = addAll(files)

        public fun replace(vararg files: String): Boolean = clear() then exclude(*files)
        public fun replace(files: Iterable<String>): Boolean = clear() then exclude(files)
    }

    public val extensions: AllowedExtensions
    public val excludes: ExcludedFiles

    public fun extensions(block: Action<AllowedExtensions>): Unit = block.execute(extensions)
    public fun excludes(block: Action<ExcludedFiles>): Unit = block.execute(excludes)
}