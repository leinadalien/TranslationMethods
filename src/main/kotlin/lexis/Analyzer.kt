package lexis

import lexis.token.Token
import lexis.token.TokenType
import java.io.FileReader

class Analyzer(fileName: String) {
    var tokens: List<Token>
        private set
    init {
        val input = FileReader(fileName).readText()
        tokens = tokenize(input)
        checkForMistake(tokens)
    }
    companion object {
        val KEYWORDS = listOf(
            "auto",
            "break",
            "case",
            "char",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extern",
            "float",
            "for",
            "goto",
            "if",
            "int",
            "long",
            "register",
            "return",
            "short",
            "signed",
            "unsigned",
            "sizeof",
            "static",
            "struct",
            "switch",
            "typedef",
            "union",
            "unsigned",
            "void",
            "volatile",
            "while"
        )
        private val TOKEN_REGEX = Regex("\"([^\"]*)\"|'[^']'|\\+\\+|--|==|!=|<=|>=|\\|\\||&&|[=+\\-*/%&^|<>!~?]|[\\w.\"'-]+|[()\\[\\]{};:,.`]")
    }



    private fun tokenize(input: String): List<Token> {
        return TOKEN_REGEX.findAll(input).map { matchResult ->
            val lineNumber = input.substring(0, matchResult.range.first).count { it == '\n' } + 1
            val columnNumber = matchResult.range.first - input.lastIndexOf('\n', matchResult.range.first - 1)
            Token(matchResult.value.trim(), Token.Position(lineNumber, columnNumber))
        }.toList()
    }

    private fun checkForMistake(tokens: Collection<Token>) {
        for (token in tokens) {
            if (token.type is TokenType.Unknown ||
                token.type is TokenType.Punctuation.Quote) throw LexicalException(token)
        }
    }

    fun printTable(uniqueTokens: Boolean = false) {
        val table = ConsoleTable("№","Token", "Type", "Position")
        var order = 1
        for (token in if (uniqueTokens) tokens.toSet() else tokens) {
            table.addRow( order++, token.value, token.type, token.position)
        }
        println(table)
    }
}