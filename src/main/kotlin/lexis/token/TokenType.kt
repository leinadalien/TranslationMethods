package lexis.token

import syntax.ast.Node

sealed class TokenType {
    object Identifier : TokenType()
    sealed class Constant : TokenType() {
        object Number : Constant()
        object Symbol : Constant()
        object Bool : Constant()
        object String : Constant()
    }
    sealed class Punctuation : TokenType() {
        sealed class Paren : Punctuation() {
            object Left : Paren()
            object Right : Paren()
            override fun toString() = (this::class.simpleName ?: "") + ' ' + javaClass.superclass.simpleName
        }
        sealed class Brace : Punctuation() {
            object Left : Brace()
            object Right : Brace()
            override fun toString() = (this::class.simpleName ?: "") + ' ' + javaClass.superclass.simpleName
        }
        sealed class Bracket : Punctuation() {
            object Left : Bracket()
            object Right : Bracket()
            override fun toString() = (this::class.simpleName ?: "") + ' ' + javaClass.superclass.simpleName
        }
        object Colon : Punctuation()
        object Comma : Punctuation()
        object Semicolon : Punctuation()
        sealed class Quote : Punctuation() {
            object Single : Quote()
            object  Double : Quote()
            override fun toString() = (this::class.simpleName ?: "") + ' ' + javaClass.superclass.simpleName
        }
    }
    sealed class Operator : TokenType() {

        object Not : Operator()
        sealed class Binary : Operator() {
            object Addictive : Binary()
            object Multiply : Binary()
            object Logical : Binary()
            object Comparison : Binary()
        }
        object IncDec : Operator()
        object Other : Operator()
        override fun toString() = (this::class.simpleName ?: "") + ' ' + javaClass.superclass.simpleName
    }
    object Keyword : TokenType()
    object Unknown : TokenType()
    override fun toString() = this::class.simpleName ?: ""
}