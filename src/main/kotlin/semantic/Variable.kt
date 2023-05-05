package semantic

import lexis.token.TokenType
import syntax.ast.Node

class Variable( val type: Node.Type, val name: String, var value: Node.Expression.Constant? = null)