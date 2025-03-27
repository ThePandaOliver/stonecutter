# Frequently asked questions
## Does it support Groovy/Kotlin buildscripts?
> Using Kotlin DSL is recommended for Stonecutter projects.
> Kotlin buildscripts have better IDE integration with method completion and documentation.
>
> Groovy DSL is still supported, but may be more confusing to work with.

## What languages does Stonecutter work with?
> Primarily Java and Kotlin, but in general any file format with `//` and `/* */` comment blocks.
> Comment syntax is the same for any used language.

## Can I use it for projects other than Fabric mods?
> Yes! Stonecutter doesn't depend on the platform you use,
so it works for Forge, NeoForge, Paper, and even non-modding projects.
>
> Examples are given for a Fabric mod to provide some real examples,
> but if you're using a different platform, apply the described steps to your needs.

## Can I bundle all built versions into one mod?
> In theory, yes, in practice - don't.
>
> This is highly discouraged because it increases the filesize of the mod by `n` times the versions you have,
> which is a big redundancy with no benefit.  
> Minecraft content websites like Modrinth and Curseforge select the correct build of the mod for the Minecraft
> version you need, and you can use Gradle scripts or GitHub actions to automatically publish all builds.  
> The bundle implementation is highly dependent on the platform you use and the functionality of your mod, so 
> if you're dedicated to doing it - you're on your own adventure.

## How is it different to the Preprocessor?
> There are quite a lot of improvements. Some key points are:
> - Optimization: Stonecutter provides faster version switching and building.
> - No remapping: Stonecutter doesn't remap your code when switching versions, avoiding issues Preprocessor has with it.
> - Cleaner comment syntax: Stonecutter disables blocks of code using multiline comments, instead of `//$$` on every line.
> - Comprehensive syntax: Stonecutter comments allow complex conditions and scopes for targeting specific blocks of code.
> - Semantic version comparisons: Stonecutter operates on semantic versions, which makes it less error-prone.
> - Inline conditions: `method(/*? If >1.20 {*/ param /*?}*/)` - allows for modifying small chunks of code.
> - Condition parameters: Stonecutter supports user constants, which can be checked in code - `//? if fabric {`.
> - Swap blocks: For common replacements, Stonecutter provides syntax to insert predetermined code depending on the version.
> - Comprehensive errors: Stonecutter implements a complete parser to process comments, which can nicely report syntax errors if they occur.
> - Project setup: Stonecutter is easier to set up for new and existing projects.
>
> The result is:
> - Versioned code is easier and faster to write and test.
> - Multi-loader setups are easier (some are using stonecutter only as a multi-loader setup, without multi-version, shocking).
> - Semantic version operations in buildscript allow fine-tuned configurations.
> - The setup system allows easily creating versioned subprojects.
>
> However, some functionality of Preprocessor is not supported:
> - The syntax is not cross-compatible, meaning adding Stonecutter to a Preprocessor project might be challenging.
    > You can use structural find and replace in your IDE to convert it, but it will still require verifying the code.
> - No source remapping. If you rely on this feature, you would need to do it manually with Stonecutter.  
    > *(Won't be implemented because of the language-independence Stonecutter has)*
> - No `@Pattern` annotation. You can replace those with swaps for the most part.  
    > *(Won't be implemented for the same reasons as above)*
> - No versioned file overrides. There are techniques to have versioned resource files, such as access wideners.  
    > *(Such files are confusing to people unfamiliar with the codebase, so there are no plans to implement them, unless a good solution is found)*

## Contact
If you have questions, contact me on [Discord](https://discord.gg/TBgNUCfryS)
or email me at `git.kikugie@protonmail.com` (KikuGie she/her)

