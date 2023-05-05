package syntax.exceptions

import lexis.token.Token

class UnexpectedTokenException(val expected: String, private val foundToken: Token) : SyntaxException() {
    val position = foundToken.position
    override val message: String
        get() = "Expected '$expected', but found '${foundToken.value}' at position ${position.line}:${position.column}"
}