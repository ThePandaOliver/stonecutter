# Stonecutter parameters

## Condition constants
Stonecutter allows providing boolean values to be used in the comment syntax.
The common use cases are multi-loader setups or testing builds.

> [!NOTE]
> Constants must start with an underscore or an English letter,
> after which they may contain those characters, as well as numbers and dashes.

### Constant specification
::: tabs key:dsl
== build.gradle.kts
```kotlin
stonecutter {
    val is21: Boolean = eval(current.version, ">=1.21")
    val is20: Boolean = eval(current.version, ">=1.20")

    // As provider
    const("const1") {
        eval(current.version, ">=1.20")
    }
    
    // As single functions
    const("const1", is20)
    const("const2", is21)
    
    // With multiple parameters
    consts("const1" to is20, "const2" to is21)
    
    // Via property assignment
    consts["const1"] = is20
    consts["const2"] = is21
    
    consts += mapOf(
        "const1" to is20, 
        "const2" to is21
    )
}
```

== build.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
stonecutter {
    def is21 = eval(current.version, ">=1.21")
    def is20 = eval(current.version, ">=1.20")

    // As provider
    const("const1") {
        eval(current.version, ">=1.20")
    }

    // As single functions
    const("const1", is20)
    const("const2", is21)

    // With multiple parameters
    consts("const1" to is20, "const2" to is21)
}
```
:::

### Choice selector
Choice selectors can be used to define multiple constants in one expression,
which is often used in multi-loader setups to create mod loader constants.
::: tabs key:dsl
== build.gradle.kts
```kotlin
stonecutter {
    val current = "option #2"
    consts(
        current, 
        "option #1", // != "option #2" -> false
        "option #2", // == "option #2" -> true
        "option #3", // != "option #2" -> false
    )
}
```

== build.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
stonecutter {
    def current = "option #2"
    consts(
        current, 
        "option #1", // != "option #2" -> false
        "option #2", // == "option #2" -> true
        "option #3", // != "option #2" -> false
    )
}
```
:::

### Comment syntax
Constants are referenced by their name in comments:
```java [example.java]
public static void example() {
    //? if my_const {
    MinecraftClass.method();
    //?}
}
```

## Condition dependencies
When writing `? if >=1.20` in a Stonecutter comment, the last part is a version predicate.
In this case the target the predicate is used against is implicit, using the Minecraft version
specified in [project settings](../start/settings).

You can write this check explicitly with the **assign** operator: `? if minecraft: >=1.20`,
which in this case has the same output.

You can also add custom predicate targets, which can be used for parts of your code
that depend on libraries that may not update as soon as Minecraft releases new versions.

> [!NOTE]
> Dependency names follow the same naming requirements as [constants](#condition-constants).
> The provided version must either be a constant-like string or a valid [semantic version](https://semver.org/).

### Dependency specification
::: tabs key:dsl
== build.gradle.kts
```kotlin
stonecutter {
    dependency("mod_menu", property("mod_menu_version") as String)
    
    // Dependencies have the same kinds of assignment functions as constants
}
```

== build.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
stonecutter {
    dependency("mod_menu", project.mod_menu_version.toString())

    // Dependencies have the same kinds of assignment functions as constants
}
```
:::

### Comment syntax
```java
//? if mod_menu: >=1.0 {
public class ModMenuIntegration {
    // ...
}
//?}
```

## String swaps
While writing versioned code, you may need to modify the same fragment in many places, 
which adds a lot of boilerplate.

Swaps allow evaluating the condition before processing code and inserting the required code block.
The following condition will be used as an example:
::: tabs key:vers
== With: 1.20.1
```java {3,10}
public static void example1() {
    //? if <1.21 {
    method1();
    //?} else
    /*method2();*/
}

public static void example2() {
    //? if <1.21 {
    method1();
    //?} else
    /*method2();*/
}
```
== With: 1.21.1
```java {5,12}
public static void example1() {
    //? if <1.21 {
    /*method1();
    *///?} else
    method2();
}

public static void example2() {
    //? if <1.21 {
    /*method1();
    *///?} else
    method2();
}
```
:::

In this case a swap can be used to determine the content.

### Swap specification
::: tabs key:dsl
== build.gradle.kts
```kotlin
stonecutter {
    swap(
        "my_swap", 
        if (eval(current.version, "<1.21")) "method1();"
        else "method2();"
    )

    swaps["my_swap"] = when {
        eval(current.version, "<1.21") -> "method1();"
        else -> "method2();"
    }
    
    // Swaps have the same kinds of assignment functions as constants
}
```

== build.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
stonecutter {
    swap(
        "my_swap",
        eval(current.version, "<1.21") ? "method1();" : "method2();"
    )
    
    // Swaps have the same kinds of assignment functions as constants
}
```
:::

### Comment syntax
The example method call will be replaced with a new string based 
on the provided state

::: tabs key:vers
== With: 1.20.1
```java {3,8}
public static void example1() {
    //$ my_swap
    method1();
}

public static void example2() {
    //$ my_swap
    method1();
}
```
== With: 1.21.1
```java {3,8}
public static void example1() {
    //$ my_swap
    method2();
}

public static void example2() {
    //$ my_swap
    method2();
}
```
:::

## Replacements
**TBA**

## Global parameters
### Stonecutter properties
These properties affect the entire processing flow for the versioned project.

- **`debug: Boolean`**:
> *Default: `false`*;  
> Enables Stonecutter debug mode. The specifics of this feature may
> change without notice, so only do it when you're asked to do it.

- **`processFiles: Boolean = true`**:
> *Default: `false`*;  
> Entirely disables file processing, which can be useful if you only need 
> to compile your code against different Java or dependency versions.

- **`defaultReceiver: String`**:
> *Default: `"minecraft"`*;  
> Changes the default explicit receiver for predicate checks.
> Default value results in the behaviour described in [dependencies section](#condition-dependencies).

- **`generateRunConfigs: Collection<RunConfigType>`**:
> *Default: `setOf(RunConfigType.SWITCH, RunConfigType.CHISEL)`*;  
> Changes, which types of run configurations are generated for IntelliJ IDEA.
> Default creates entries for version switching and all chiseled tasks.

### Build parameters
File processor parameters, such as constant, swaps, and others described above
can be specified in `stonecutter.gradle[.kts]` for every version.

::: tabs key:dsl
== stonecutter.gradle.kts
```kotlin
stonecutter parameters {
    consts["is_active"] = metadata.isActive
    dependencies["my_dependency"] = node
        ?.let { it.project.property("my_dependency_version") as String }
        ?: "0.0"
    // ...
}
```

== stonecutter.gradle
> [!IMPORTANT]
> Stonecutter support and documentation for Groovy buildscripts are limited.
> See the [FAQ page](/wiki/faq#groovy-support) for more information.
```groovy
stonecutter.parameters {
    const("is_active", metadata.isActive)
    dependency("my_dependency", node != null 
        ? node.project.property("my_dependency_version").toString()
        : "0.0"
    )
    // ...
}
```
:::

> [!NOTE]
> In the `parameters {}` scope, `node.project` refers to the Gradle project for that Stonecutter version.
> 
> However, in some multi-loader setups that project may not exist and `node` will be `null`.
> In that case you should still provide a default value for the corresponding parameter.

## Condition syntax
Stonecutter conditions provide functionality beyond single-value checks,
which can be used to reduce comment nesting.

### Version predicates
Stonecutter supports a variety of version comparison predicates:
- `=`, `<`, `>`, `<=`, `>=` - basic equality checks.
- `~` - checks if the major and minor versions are equal. (I.e. `1.2.3 ~1.2.1`)
- `^` - checks if the major versions are equal. (I.e. `1.2.3 ^1.4`)
- ` ` - (no operator) same as the `=` check. (So you can write `if 1.20` instead of `if =1.20`)

Stonecutter **doesn't support** the following SemVer predicates:
- `x` - (as in `1.20.x`) these can be replaced with `~` or `^` operator for most cases.
- `*` - ('any' predicate) useless for Stonecutter, as such condition would always succeed and should be removed in that case.

Predicates can be chained, in which case all must pass to satisfy the condition.
```java
public static void example() {
    //? if ~1.20 <1.20.4
    method();
}
```

### Logical operators
Constants and version predicates are evaluated as boolean values.
You can combine them with the standard set of logical operators.
- `!` - negation: `if !fabric`, `if !=1.20`
- `||` - union (or): `if fabric || forge`, `if 1.20 || 1.21`
- `&&` - intersection (and): `if 1.20 && fabric`
- `( )` - grouping: `if fabric && (1.20 || 1.21)`

### Nested conditions
Conditional blocks can be nested within each other **up to 10 times**.
To avoid issues with multi-line comments, `/* */` replaced with `/^ ^/`,
which may also include superscript numbers to correctly manage the blocks.
::: tabs key:vers
== With: 1.20.1
```java [example.java]
//? if <1.21 {
method1();
    //? if <1.20 {
    method2();
        //? if <1.19 {
        method3();
        //?}
    //?}
//?}
```
== With: 1.21.1
```java [example.java]
//? if <1.21 {
/*method1();
    //? if <1.20 {
    /^method2();
        //? if <1.19 {
        /^¹method3();
        ¹^///?}
    ^///?}
*///?}
```
:::