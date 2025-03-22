package dev.kikugie.stwotcher.exec.eval

import dev.kikugie.semver.VersionParser
import dev.kikugie.stwotcher.data.param.ProcessParameters
import dev.kikugie.stwotcher.data.token.ComponentToken
import dev.kikugie.stwotcher.data.type.OperatorType

class ConditionChecker(val params: ProcessParameters) : ComponentToken.Visitor<Boolean> {
    private fun failure(message: String): Nothing = throw IllegalArgumentException(message)

    override fun visitPlaceholder(it: ComponentToken.Placeholder): Nothing {
        throw UnsupportedOperationException("Malformed conditions shouldn't be evaluated")
    }

    override fun visitGroup(it: ComponentToken.Group): Boolean = it.body.accept(this)

    override fun visitUnary(it: ComponentToken.Unary): Boolean = when (it.operator.type) {
        OperatorType.NEGATE -> it.right.accept(this).not()
        else -> failure("Invalid unary operator: ${it.operator}")
    }

    override fun visitBinary(it: ComponentToken.Binary): Boolean = when (it.operator.type) {
        OperatorType.AND -> it.left.accept(this) && it.right.accept(this)
        OperatorType.OR -> it.left.accept(this) || it.right.accept(this)
        else -> failure("Invalid binary operator: ${it.operator}")
    }

    override fun visitConstant(it: ComponentToken.Constant): Boolean =
        requireNotNull(params.constants[it.value]) { "Constant ${it.value} not found" }

    override fun visitAssignment(it: ComponentToken.Assignment): Boolean {
        val target =
            if (it.target != null) requireNotNull(params.dependencies[it.target.value]) { "Dependency ${it.target.value} not found" }
            else requireNotNull(params.dependencies[""]) { "Missing default dependency"}
        return it.predicates.all {
            VersionParser.parsePredicate(it.value).value.eval(target)
        }
    }
}