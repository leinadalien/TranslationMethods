package semantic.exceptions

import lexis.token.Token
import semantic.Variable
import syntax.ast.Node

class TypeMismatch(val required: Node.Type?, val found: String, val position: Token.Position?) : SemanticException() {
    override val message: String
        get() = "Required: ${required?.typeSpecifier?.token?.value ?: "void" }, but found $found on position ${position?.line}:${position?.column}"
}