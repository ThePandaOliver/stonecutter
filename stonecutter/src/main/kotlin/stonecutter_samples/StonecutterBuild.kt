@file:Suppress("unused", "ClassName", "FunctionName", "UNUSED_PARAMETER")

package stonecutter_samples

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import kotlin.collections.set
import kotlin.to

private val stonecutter: StonecutterBuildExtension get() = TODO("This is a sample, it must not be instantiated.")
private fun stonecutter(action: StonecutterBuildExtension.() -> Unit) {}
private fun property(name: String): Any {
    throw UnsupportedOperationException("Not yet implemented.")
}

private object swaps {
    fun setter() {
        stonecutter {
            swaps["my_swap"] = when {
                eval(current.version, ">=1.21") -> "replacement #1"
                eval(current.version, ">=1.20") -> "replacement #2"
                else -> "replacement #3"
            }
        }
    }

    fun single() {
        stonecutter {
            val replacement = if (eval(current.version, ">=1.21")) "replacement #1" else "replacement #2"
            swap("my_swap", replacement)
        }
    }

    fun provider() {
        stonecutter {
            swap("my_swap") {
                if (eval(current.version, ">=1.21")) "replacement #1"
                else "replacement #2"
            }
        }
    }

    fun vararg() {
        stonecutter {
            val options = if (eval(current.version, ">=1.21")) "option #1" else "option #2"
            val variables = if (eval(current.version, ">=1.21")) "variable #1" else "variable #2"
            swaps("my_swap" to options, "my_other_swap" to variables)
        }
    }

    fun iterable() {
        stonecutter {
            val replacements = mutableListOf<Pair<String, String>>()
            replacements.add("my_swap" to if (eval(current.version, ">=1.21")) "option #1" else "option #2")
            replacements.add("my_other_swap" to if (eval(current.version, ">=1.21")) "variable #1" else "variable #2")
            swaps(replacements)
        }
    }

    fun map() {
        stonecutter {
            val replacements = mapOf(
                "my_swap" to if (eval(current.version, ">=1.21")) "option #1" else "option #2",
                "my_other_swap" to if (eval(current.version, ">=1.21")) "variable #1" else "variable #2"
            )
            swaps(replacements)
        }
    }
}

private object constants {
    fun setter() {
        stonecutter {
            consts["my_const"] = eval(current.version, ">=1.21")
        }
    }

    fun single() {
        stonecutter {
            val state = eval(current.version, ">=1.21")
            const("my_const", state)
        }
    }

    fun provider() {
        stonecutter {
            const("my_const") {
                eval(current.version, ">=1.21")
            }
        }
    }

    fun vararg() {
        stonecutter {
            val is121 = eval(current.version, ">=1.21")
            val is120 = eval(current.version, ">=1.20")
            consts("my_const" to is121, "my_other_const" to is120)
        }
    }

    fun iterable() {
        stonecutter {
            val constants = mutableListOf<Pair<String, Boolean>>()
            constants.add("my_const" to eval(current.version, ">=1.21"))
            constants.add("my_other_const" to eval(current.version, ">=1.20"))
            consts(constants)
        }
    }

    fun map() {
        stonecutter {
            val constants = mapOf(
                "my_const" to eval(current.version, ">=1.21"),
                "my_other_const" to eval(current.version, ">=1.20")
            )
            consts(constants)
        }
    }

    fun choices_vararg() {
        stonecutter {
            val current = "option #2"
            consts(current, "option #1", "option #2", "option #3")
        }
    }

    fun choices_iterable() {
        stonecutter {
            val options = buildList { repeat(3) { add("option #$it") } }
            val current = options[1]
            consts(current, options)
        }
    }
}

private object dependencies {
    fun setter() {
        stonecutter {
            dependencies["my_dependency"] = property("dependency") as String
        }
    }

    fun single() {
        stonecutter {
            val dependency = property("dependency") as String
            dependency("my_dependency", dependency)
        }
    }

    fun provider() {
        stonecutter {
            dependency("my_dependency") {
                property("dependency") as String
            }
        }
    }

    fun vararg() {
        stonecutter {
            dependencies(
                "my_dependency" to property("dependency") as String,
                "my_other_dependency" to property("other_dependency") as String
            )
        }
    }

    fun iterable() {
        stonecutter {
            val dependencies = mutableListOf<Pair<String, String>>()
            dependencies.add("my_dependency" to property("dependency") as String)
            dependencies.add("my_other_dependency" to property("other_dependency") as String)
            dependencies(dependencies)
        }
    }

    fun map() {
        stonecutter {
            val dependencies = mapOf(
                "my_dependency" to property("dependency") as String,
                "my_other_dependency" to property("other_dependency") as String
            )
            dependencies(dependencies)
        }
    }
}

private object allowExtensions {
    fun vararg() {
        stonecutter.allowExtensions("yml", "yaml")
    }

    fun iterable() {
        val extensions = listOf("yml", "yaml")
        stonecutter.allowExtensions(extensions)
    }
}

private object overrideExtensions {
    fun vararg() {
        stonecutter.overrideExtensions("scala", "sc")
    }

    fun iterable() {
        val extensions = listOf("scala", "sc")
        stonecutter.overrideExtensions(extensions)
    }
}

private object excludeFiles {
    fun vararg() {
        stonecutter.excludeFiles("src/main/resources/properties.json5")
    }

    fun iterable() {
        val files = listOf("src/main/resources/properties.json5")
        stonecutter.excludeFiles(files)
    }
}

private object replacements {
    fun string_basic() {
        /* Creates multiple replacements without ambiguity
           1.21: ['A', 'B'] -> 'C'
           1.20: ['A', 'C'] -> 'B'
           1.19: ['B', 'C'] -> 'A'
         */
        stonecutter {
            replacement(eval(current.version, "<1.21"), "A", "B")
            replacement(eval(current.version, "<1.20"), "B", "C")
        }
    }

    fun string_ambiguous() {
        /* Created replacements create two possible outcomes when switching to 1.20, and an exception is thrown
           1.21: ['A', 'B'] -> 'C'
           1.20: ['B'] -> 'A' and ['B'] -> 'C' !!!
           1.19: ['B', 'C'] -> 'A'
         */
        stonecutter {
            replacement(eval(current.version, ">=1.21"), "A", "B")
            replacement(eval(current.version, ">=1.20"), "B", "C")
        }
    }

    fun string_circular() {
        /* Created replacements create circular references when switching to 1.21 or 1.20, and an exception is thrown.
           1.21: ['B'] -> 'A' and ['A'] -> 'B' !!!
           1.20: ['A'] -> 'B'
           1.19: ['A'] -> 'B' and ['B'] -> 'A' !!!
         */
        stonecutter {
            replacement(eval(current.version, "<1.21"), "A", "B")
            replacement(eval(current.version, "<1.20"), "B", "A")
        }
    }

    fun string_phased() {
        /* Replacements are created in different phases, which defaults to "LAST".
           With an example file `/*$ my_swap*/ A`:
           1.20:
           - Replace A -> B
           - Swap with C
           - Replace C -> D
           1.21:
           - Replace B -> A (nothing happens)
           - Swap with A
           - Replace D -> C (nothing happens)
           Phases can be used to affect other comments, but at a risk of non-reversible transformations.
         */
        stonecutter {
            replacement(eval(current.version, "<1.21"), "A", "B", "FIRST")
            replacement(eval(current.version, "<1.21"), "C", "D")

            swap("my_swap") {
                if (eval(current.version, "<1.21")) "C"
                else "A"
            }
        }
    }

    fun string_identified() {
        /* Replacements can be given identifiers, which allows them to be enabled for specific files.
           The replacement tokens must be at the top of the file. For example:
           ```
           //~ repl_token
           A
           /*~ repl_token_#2*/ // this token comes after content and will not be included
           ```
           With token:
           1.20: ['A', 'B'] -> 'C'
           1.21: ['B', 'C'] -> 'A'
           Without token:
           1.20: ['A'] -> 'B'
           1.21: ['B'] -> 'A'
         */
        stonecutter {
            replacement(eval(current.version, "<1.21"), "A", "B")
            replacement(eval(current.version, "<1.21"), "B", "C", identifier = "repl_token")
            replacement(eval(current.version, "<1.21"), "C", "D", identifier = "repl_token_#2")
        }
    }

    fun string_configuration() {
        stonecutter {
            stringReplacement {
                direction = eval(current.version, "<1.21")
                source = "A"
                target = "B"
                // optional
                phase = "FIRST"
                identifier = "repl_token"
            }
        }
    }

    fun regex_basic() {
        stonecutter {
            replacement(eval(current.version, "<1.21"), "[AB]", "C", "[BC]", "A")
        }
    }

    fun regex_configuration() {
        stonecutter {
            regexReplacement {
                direction = eval(current.version, "<1.21")
                sourcePattern = "[AB]"
                targetValue = "C"
                targetPattern = "[BC]"
                sourceValue = "A"
            }
        }
    }

    fun dynamic_assign() {
        // To be used with Kotlin DSL
        stonecutter {
            // Adds a string replacement
            replacements += mapOf(
                "direction" to eval(current.version, "<1.21"),
                "source" to "A",
                "target" to "B",
                // optional
                "phase" to "FIRST",
                "identifier" to "repl_token",
            )

            // Adds a regex replacement
            replacements += mapOf(
                "direction" to eval(current.version, "<1.21"),
                "sourcePattern" to "A",
                "targetValue" to "B",
                "targetPattern" to "B",
                "sourceValue" to "A",
                // optional
                "phase" to "FIRST",
                "identifier" to "repl_token",
            )
        }
    }

    fun dynamic_map() {
        // To be used with Groovy DSL
        stonecutter {
            // Adds a string replacement
            replacement(mapOf(
                "direction" to eval(current.version, "<1.21"),
                "source" to "A",
                "target" to "B",
                // optional
                "phase" to "FIRST",
                "identifier" to "repl_token",
            ))

            // Adds a regex replacement
            replacement(mapOf(
                "direction" to eval(current.version, "<1.21"),
                "sourcePattern" to "A",
                "targetValue" to "B",
                "targetPattern" to "B",
                "sourceValue" to "A",
                // optional
                "phase" to "FIRST",
                "identifier" to "repl_token",
            ))
        }
    }
}