import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.StringVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionPredicate
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf

private infix fun Result<*>.shouldFailWith(message: String) = exceptionOrNull() should {
    withClue({ "Received success ${getOrNull()}" }) { it.shouldNotBeNull() }
    it!!.message shouldBe message
}

private infix fun <T> Result<T>.shouldSucceed(action: (T) -> Unit) = getOrNull() should {
    withClue({ "Received failure ${exceptionOrNull()}" }) { it.shouldNotBeNull() }
    action(it.shouldNotBeNull())
}

class StringVersionTest : StringSpec({
    fun MutableMap<String, () -> Unit>.locate(input: String, action: (Int) -> Unit) {
        this["locate: '$input'"] = { action(StringVersion.locate(input)) }
    }

    fun MutableMap<String, () -> Unit>.parse(input: String, action: (Result<StringVersion>) -> Unit) {
        this["parse: '$input'"] = { action(StringVersion.parse(input)) }
    }

    withData(buildMap {
        locate("identifier") { it shouldBe 10 }
        locate("identifier gap") { it shouldBe 10 }
        locate("1.2.3") { it shouldBe 1 }

        parse("") { it shouldFailWith "Version string cannot be empty" }
        parse("1.2.3") { it shouldFailWith "Version string contains invalid characters" }
    }) { it() }
})

class SemanticVersionTest : StringSpec({
    fun MutableMap<String, () -> Unit>.locate(input: String, action: (Int) -> Unit) {
        this["locate: '$input'"] = { action(SemanticVersion.locate(input)) }
    }

    fun MutableMap<String, () -> Unit>.parse(input: String, action: (Result<SemanticVersion>) -> Unit) {
        this["parse: '$input'"] = { action(SemanticVersion.parse(input)) }
    }

    withData(buildMap {
        parse("1.2.3.4") { it shouldSucceed { it.components shouldBeSameSizeAs intArrayOf(1, 2, 3, 4) } }

        parse("1-a") { it shouldSucceed { it.preRelease shouldBe "a" } }
        parse("1.2-a.b-c") { it shouldSucceed { it.preRelease shouldBe "a.b-c" } }

        parse("1+a") { it shouldSucceed { it.buildMetadata shouldBe "a" } }
        parse("1.2+a.b-c") { it shouldSucceed { it.buildMetadata shouldBe "a.b-c" } }

        parse("") { it shouldFailWith "Version string cannot be empty" }
        parse("id") { it shouldFailWith "Invalid version component" }
        parse(".1") { it shouldFailWith "Invalid version component" }
        parse("1.") { it shouldFailWith "Invalid version component" }
        parse("1..1") { it shouldFailWith "Invalid version component" }
        parse("1.a.1") { it shouldFailWith "Invalid version component" }

        parse("1-") { it shouldFailWith "Empty pre-release modifier" }
        parse("1.2-+") { it shouldFailWith "Empty pre-release modifier" }
        parse("1-/a") { it shouldFailWith "Invalid pre-release modifier" }

        parse("1+") { it shouldFailWith "Empty build metadata" }
        parse("1.2++") { it shouldFailWith "Invalid build metadata" }
        parse("1+/a") { it shouldFailWith "Invalid build metadata" }
    }) { it() }
})

class LenientVersionTest : StringSpec({
    fun MutableMap<String, () -> Unit>.locate(input: String, action: (Int) -> Unit) {
        this["locate: '$input'"] = { action(Version.locate(input)) }
    }

    fun MutableMap<String, () -> Unit>.parse(input: String, action: (Result<Version>) -> Unit) {
        this["parse: '$input'"] = { action(Version.parse(input)) }
    }

    withData(buildMap {
        locate("1.2") { it shouldBe 3 }
        locate("abc") { it shouldBe 3 }

        parse("1.2.3") { it shouldSucceed { it should beInstanceOf<SemanticVersion>() } }
        parse("abcde") { it shouldSucceed { it should beInstanceOf<StringVersion>() } }
    }) { it() }
})

class StringPredicateTest : StringSpec({
    fun MutableMap<String, () -> Unit>.locate(input: String, action: (Int) -> Unit) {
        this["locate: '$input'"] = { action(VersionPredicate.Plain.locate(input)) }
    }

    fun MutableMap<String, () -> Unit>.parse(input: String, action: (Result<VersionPredicate>) -> Unit) {
        this["parse: '$input'"] = { action(VersionPredicate.Plain.parse(input)) }
    }

    withData(buildMap {
        locate("identifier") { it shouldBe 10 }
        locate("=identifier") { it shouldBe 11 }
        locate("= identifier") { it shouldBe 12 }
        locate("~identifier") { it shouldBe -1 }

        parse("") { it shouldFailWith "Predicate string cannot be empty" }
        parse("=") { it shouldFailWith "Version string cannot be empty" }
        parse(" =id") { it shouldFailWith "Predicate string must be trimmed" }
        parse("?id") { it shouldFailWith "Invalid predicate operator '?'" }
        parse("~id") { it shouldFailWith "Operator '~' can't be used on a string version" }
        parse("^id") { it shouldFailWith "Operator '^' can't be used on a string version" }
    }) { it() }
})

class SemanticPredicateTest : StringSpec({
    fun MutableMap<String, () -> Unit>.locate(input: String, action: (Int) -> Unit) {
        this["locate: '$input'"] = { action(VersionPredicate.Semantic.locate(input)) }
    }

    fun MutableMap<String, () -> Unit>.parse(input: String, action: (Result<VersionPredicate>) -> Unit) {
        this["parse: '$input'"] = { action(VersionPredicate.Semantic.parse(input)) }
    }

    withData(buildMap {
        locate("1.2.3") { it shouldBe 5 }
        locate("=1.2.3") { it shouldBe 6 }
        locate("~1.2.3") { it shouldBe 6 }
        locate("= 1.2.3") { it shouldBe 7 }

        parse("") { it shouldFailWith "Predicate string cannot be empty" }
        parse("=") { it shouldFailWith "Version string cannot be empty" }
        parse(" =1.2.3") { it shouldFailWith "Predicate string must be trimmed" }
        parse("= id") { it shouldFailWith "Invalid version component" }
        parse("?1.2.3") { it shouldFailWith "Invalid predicate operator '?'" }
    }) { it() }
})

class LenientPredicateTest : StringSpec({
    fun MutableMap<String, () -> Unit>.locate(input: String, action: (Int) -> Unit) {
        this["locate: '$input'"] = { action(VersionPredicate.locate(input)) }
    }

    fun MutableMap<String, () -> Unit>.parse(input: String, action: (Result<VersionPredicate>) -> Unit) {
        this["parse: '$input'"] = { action(VersionPredicate.parse(input)) }
    }

    withData(buildMap {
        locate("=1.2.3") { it shouldBe 6 }
        locate("=abc") { it shouldBe 4 }

        parse("=1.2.3") { it shouldSucceed { it.version should beInstanceOf<SemanticVersion>() } }
        parse("=abc") { it shouldSucceed { it.version should beInstanceOf<StringVersion>() } }

        parse("~abc") { it shouldFailWith "Operator '~' can't be used on a string version" }
        parse("^abc") { it shouldFailWith "Operator '^' can't be used on a string version" }
    }) { it() }
})