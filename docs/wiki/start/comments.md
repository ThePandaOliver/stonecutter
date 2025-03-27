# Stonecutter comments
## Introduction
Stonecutter allows adjusting the source code of your mod to the corresponding
Minecraft version using in-comment statements.

This way it's compatible with all IDEs by default, since they see Stonecutter
conditions as regular comments.

*Currently, Stonecutter can read comments in `//` and `/* */` blocks, with support
for different formats coming in future releases.*

## Creating conditions
### Simple check
A basic versioned condition looks like this:
::: tabs key:vers
== With: 1.20.1
```java
public static void example() {
    //? if >=1.21 {
    /*System.out.println("We have trial chambers!");
    System.out.println("Let's put our maces to their faces!");
    *///?}
}
```
== With: 1.21.1
```java {3-4}
public static void example() {
    //? if >=1.21 {
    System.out.println("We have trial chambers!");
    System.out.println("Let's put our maces to their faces!");
    //?}
}
```
== With: 1.21.4
```java {3-4}
public static void example() {
    //? if >=1.21 {
    System.out.println("We have trial chambers!");
    System.out.println("Let's put our maces to their faces!");
    //?}
}
```
:::

Breaking it down:
```text
? if >=1.21 {
| |  |      ^ specify the start of the commented block
| |  ^ the Minecraft version predicate
| ^ (optional) improve comment readability   
^ mark the comment as a Stonecutter condition

}
^ specify the end of the comment block
```

### Condition branching
Similar to other programming languages, Stonecutter conditions support
branching with `else` and `else if` (or `elif`):
::: tabs key:vers
== With: 1.20.1
```java {3}
public static void example() {
    //? if =1.20.1 {
    System.out.println("Trails and Tales update just released!");
    //?} elif =1.21.1 {
    /*System.out.println("Tricky Trials update just released!");
    *///?} else if =1.21.4 {
    /*System.out.println("The Garden Awakens .. drop .. just dropped...");
    *///?}
}
```
== With: 1.21.1
```java {5}
public static void example() {
    //? if =1.20.1 {
    /*System.out.println("Trails and Tales update just released!");
    *///?} elif =1.21.1 {
    System.out.println("Tricky Trials update just released!");
    //?} else if =1.21.4 {
    /*System.out.println("The Garden Awakens .. drop .. just dropped...");
    *///?}
}
```
== With: 1.21.4
```java {7}
public static void example() {
    //? if =1.20.1 {
    /*System.out.println("Trails and Tales update just released!");
    *///?} elif =1.21.1 {
    /*System.out.println("Tricky Trials update just released!");
    *///?} else if =1.21.4 {
    System.out.println("The Garden Awakens .. drop .. just dropped...");
    //?}
}
```
:::

### Flexible conditions
Comment syntax is very flexible and has various features that
improve readability and development convenience.

#### Inline comments
When you need to modify a small part of the code,
conditional comments can be inserted right in the place they are needed:

::: tabs key:vers
== With: 1.20.1
```java {2}
public static void example() {
    MinecraftClass.method(/*? <=1.21.1 {*/ null /*} else {*//* 1.0 *//*?}*/);
}
```
== With: 1.21.1
```java {2}
public static void example() {
    MinecraftClass.method(/*? <=1.21 {*/ null /*} else {*//* 1.0 *//*?}*/);
}
```
== With: 1.21.4
```java {2}
public static void example() {
    MinecraftClass.method(/*? <=1.21 {*//* null *//*} else {*/ 1.0 /*?}*/);
}
```
:::

#### Line scopes
In the previous example we used `{ }` to mark, which part of the code
has to be commented out. However, if these symbols are emitted, 
Stonecutter will comment only the next line:

::: tabs key:vers
== With: 1.20.1
```java {3}
public static void example() {
    //? if <1.21
    System.out.println("This version is so old, my grandpa played it in his youth");
}
```
== With: 1.21.1
```java
public static void example() {
    //? if <1.21
    /*System.out.println("This version is so old, my grandpa played it in his youth");*/
}
```
== With: 1.21.4
```java
public static void example() {
    //? if <1.21
    /*System.out.println("This version is so old, my grandpa played it in his youth");*/
}
```
:::