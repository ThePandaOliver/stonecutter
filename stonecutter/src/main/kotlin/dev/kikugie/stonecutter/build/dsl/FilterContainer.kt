package dev.kikugie.stonecutter.build.dsl

import dev.kikugie.stonecutter.SCConfiguration
import dev.kikugie.stonecutter.SCFilterSpec
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.then
import org.gradle.api.Action

@StonecutterAPI @SCConfiguration
public interface FilterContainer {
    @SCFilterSpec
    public interface AllowedExtensions : MutableSet<String> {
        public fun allow(vararg extensions: String): Boolean = addAll(extensions)
        public fun allow(extensions: Iterable<String>): Boolean = addAll(extensions)

        public fun disallow(vararg extensions: String): Boolean = removeAll(extensions)
        public fun disallow(extensions: Iterable<String>): Boolean = removeAll(extensions)

        public fun replace(vararg extensions: String): Boolean = clear() then allow(*extensions)
        public fun replace(extensions: Iterable<String>): Boolean = clear() then allow(extensions)
    }

    @SCFilterSpec
    public interface ExcludedFiles : MutableSet<String> {
        public fun exclude(vararg files: String): Boolean = addAll(files)
        public fun exclude(files: Iterable<String>): Boolean = addAll(files)

        public fun replace(vararg files: String): Boolean = clear() then exclude(*files)
        public fun replace(files: Iterable<String>): Boolean = clear() then exclude(files)
    }

    public var extensions: AllowedExtensions
    public var excludes: ExcludedFiles

    public fun extensions(block: Action<AllowedExtensions>): Unit = block.execute(extensions)
    public fun excludes(block: Action<ExcludedFiles>): Unit = block.execute(excludes)
}