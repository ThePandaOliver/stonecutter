import dev.kikugie.semver.SemanticVersionOperations
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class BoundaryTest : StringSpec({
    data class Sample(val version: String, val boundary: Int)

    withData(
        Sample("1.0.0", 5),
        Sample("1.0.0 1.2", 5),
    ) {
        SemanticVersionOperations.getVersionBoundary(it.version) shouldBe it.boundary
    }

    withData(
        Sample("1.0.0", 5),
        Sample(">1.0.0", 6),
    ) {
        SemanticVersionOperations.getPredicateBoundary(it.version) shouldBe it.boundary
    }

})