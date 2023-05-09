package syntax.exceptions

import lexis.token.Token

class UnexpectedTokenException(val expectedValue: String, private val foundToken: Token, tokenPosition: Int) :
    SyntaxException(tokenPosition) {
    override val message: String
        get() = "Expected $expectedValue, but found '${foundToken.value}' at position ${foundToken.position.line}:${foundToken.position.column}"
}