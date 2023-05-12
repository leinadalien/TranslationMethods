package lexis.token

import lexis.Analyzer

class Token(val value: String, val position: Position) {
    val type: TokenType = classify(value)

    data class Position(val line: Int, val column: Int) : Comparable<Position> {
        override fun compareTo(other: Position): Int {
            return if (line == other.line) {
                column - other.column
            } else line - other.line
        }

        override fun toString(): String {
            return "$line:$column"
        }
    }
    companion object {
        private fun classify(value: String): TokenType {
            return when {
                value.matches("-?\\d+".toRegex()) -> TokenType.Constant.Int
                value.matches("-?\\d+(?:\\.\\d+)?[fF]?".toRegex()) -> TokenType.Constant.Float
                value.matches("-?\\d+(?:\\.\\d+)?[dD]?".toRegex()) -> TokenType.Constant.Double
                value.matches("\"([^\"]*)\"".toRegex()) -> TokenType.Constant.String
                value.matches("'[^']'".toRegex()) -> TokenType.Constant.Symbol
                value.matches("true|false".toRegex()) -> TokenType.Constant.Bool
                value.matches("[a-zA-Z_]\\w*".toRegex()) -> {
                    if (value in Analyzer.KEYWORDS)
                        TokenType.Keyword
                    else
                        TokenType.Identifier
                }

                value == "=" -> TokenType.Operator.Assign
                value.matches("[+\\-]".toRegex()) -> TokenType.Operator.Binary.Addictive
                value.matches("[*/%]".toRegex()) -> TokenType.Operator.Binary.Multiply
                value.matches("==|!=|<=|>=|<|>".toRegex()) -> TokenType.Operator.Binary.Comparison
                value.matches("\\+\\+|--".toRegex()) -> TokenType.Operator.IncDec
                value.matches("\\*=|/=|\\+=|-=|%=".toRegex()) -> TokenType.Operator.Other
                value.matches("&&|\\|\\|".toRegex()) -> TokenType.Operator.Binary.Logical
                value.matches("!".toRegex()) -> TokenType.Operator.Not
                value == "&" -> TokenType.Operator.Ref
                value.matches(",".toRegex()) -> TokenType.Punctuation.Comma
                value.matches(":".toRegex()) -> TokenType.Punctuation.Colon
                value.matches(";".toRegex()) -> TokenType.Punctuation.Semicolon
                value.matches("'".toRegex()) -> TokenType.Punctuation.Quote.Single
                value.matches("\"".toRegex()) -> TokenType.Punctuation.Quote.Double
                value == "(" -> TokenType.Punctuation.Paren.Left
                value == ")" -> TokenType.Punctuation.Paren.Right
                value == "{" -> TokenType.Punctuation.Brace.Left
                value == "}" -> TokenType.Punctuation.Brace.Right
                value == "[" -> TokenType.Punctuation.Bracket.Left
                value == "]" -> TokenType.Punctuation.Bracket.Right
                else -> TokenType.Unknown
            }
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Token -> hashCode() == other.hashCode()
            else -> false
        }
    }
}