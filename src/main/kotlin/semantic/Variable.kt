package semantic

import lexis.token.Token
import lexis.token.TokenType
import semantic.exceptions.MessageException
import syntax.ast.Node

class Variable( val type: Node.Type, val name: String, var value: Node.Expression.Constant? = null) {
    companion object {
        fun functionVariable(type: Node.Type, name: String): Variable {
            return Variable(type, name, getDefaultValue(type))
        }
        private fun getDefaultValue(type: Node.Type): Node.Expression.Constant {
            val value = when(type.typeSpecifier.token.value) {
                "char" -> " "
                "int" -> "0"
                "float" -> "0f"
                "double" -> "0d"
                "long" -> "0"
                "short" -> "0"
                else -> throw MessageException("Illegal token type")
            }
            return Node.Expression.Constant(Token(value, Token.Position(0,0)))
        }
    }
}