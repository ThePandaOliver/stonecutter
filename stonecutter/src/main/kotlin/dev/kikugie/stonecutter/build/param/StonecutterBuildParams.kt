package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterUtility

/**
 * Represents context, in which file processing parameters can be configured.
 *
 * *It is made solely to combine other interfaces.*
 */
@StonecutterAPI
public interface StonecutterBuildParams :
    ConstantVariants,
    DependencyVariants,
    SwapVariants,
    ReplacementVariants,
    FilterVariants,
    StonecutterUtility