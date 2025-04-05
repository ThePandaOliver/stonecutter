# Stonecutter guide

This guide goes through the steps of setting up Stonecutter for a Fabric mod.
If you're starting a new project, using the [Stonecutter Fabric Template](https://github.com/stonecutter-versioning/stonecutter-template-fabric)
may be easier, but reading this is still recommended to understand what you're doing.
The guide assumes you start with an empty mod, like the [Fabric Template Mod](https://github.com/FabricMC/fabric-example-mod),
adding Stonecutter functionality on top.

If you're using a different platform, the principles remain the same, but you may need to adjust your code accordingly.  
*(Author's note: I can't write a guide for every kind of mod or plugin, thank you for understanding.)*

## Community templates and resources
Aside from the official documentation, there are plenty of resources created by the community,
which can be used to quickstart development or explore unique techniques.

### [Modstitch template](https://github.com/modunion/modstitch-stonecutter-template)
Multi-loader template using [Modstitch](https://modunion.github.io/modstitch-docs/),
which works without Architectury Loom.

### [Drathonix' template](https://github.com/Drathonix/rockbreaker)
Multi-loader template using [Architectury Loom](https://docs.architectury.dev/loom/introduction) in a flat Stonecutter setup,
which comes with flexible mod properties and publishing automation.

### [JavaJumper's template](https://github.com/JumperOnJava/Stonecutter-Arch-Template)
Multi-loader template using [Architectury Loom](https://docs.architectury.dev/loom/introduction) in a flat Stonecutter setup,
which allows quickly setting it up as the desired mod.

### [Friends&Foes setup](https://github.com/Faboslav/friends-and-foes)
Although not a ready-to-use template, the mod build scripts can be used as a reference for branched Stonecutter setups.