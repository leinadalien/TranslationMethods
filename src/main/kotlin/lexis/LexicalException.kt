package lexis

import lexis.token.Token
import lexis.token.TokenType

class LexicalException(private val token: Token) : Exception() {
    override val message: String
        get() =  when(token.type) {
            is TokenType.Punctuation.Quote ->
                "Unclosed quote ${token.value} on position ${token.position.line}:${token.position.column}"
            else -> "Unknown token '${token.value}' on position ${token.position.line}:${token.position.column}"
        }
}