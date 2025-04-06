package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.build.FileProcessingFilter

public interface FilterVariants {
    @StonecutterAPI
    public val filter: FileProcessingFilter

    @StonecutterAPI
    public fun allowExtensions(extensions: Iterable<String>)

    @StonecutterAPI
    public fun allowExtensions(vararg extensions: String): Unit =
        allowExtensions(extensions.asIterable())

    @StonecutterAPI
    public fun overrideExtensions(extensions: Iterable<String>)

    @StonecutterAPI
    public fun overrideExtensions(vararg extensions: String): Unit =
        overrideExtensions(extensions.asIterable())

    @StonecutterAPI
    public fun excludeFiles(files: Iterable<String>)

    @StonecutterAPI
    public fun excludeFiles(vararg files: String): Unit =
        excludeFiles(files.asIterable())
}