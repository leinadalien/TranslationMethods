package semantic

import lexis.token.TokenType
import semantic.exceptions.MessageException
import semantic.exceptions.NotDefined
import semantic.exceptions.NotInitialized
import semantic.exceptions.TypeMismatch
import syntax.SyntaxAnalyzer
import syntax.ast.Node
import java.beans.Expression
import javax.swing.plaf.nimbus.State
import kotlin.concurrent.thread
class SemanticAnalyzer(private val tree: Node.Program) {
    private val declaredVariables = mutableListOf<Variable>()
    private val declaredFunctions = mutableListOf<Node.Function>()

    constructor(fileName: String) : this(SyntaxAnalyzer(fileName).parse())

    fun checkForMistakes() {
        checkProgram(tree)
    }


    private fun checkProgram(program: Node.Program) {
        for (function in program.functions) {
            checkFunction(function)
        }
    }




    private fun checkNode(node: Node?) {
        if (node == null) return
        val fields = node.javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            if (field.get(node) is List<*>) {
                val nodes = field.get(node) as List<Node>
                for (node in nodes) {
                    checkNode(node)
                }
            } else if (field.get(node) is Node){
                checkNode(field.get(node) as Node)
            }
        }
        when (node) {
            is Node.Statement.Declaration -> {
                val type = node.type
                node.declarators.forEach {
                    val variable = Variable(type, it.name.token.value)
                    it.initializer?.let { init ->
                        checkExpression(type, init.expression)
                    }
                    if (declaredVariables.firstOrNull { v -> v.name == variable.name } != null)
                        throw MessageException("Variable ${variable.name} is already defined")
                    declaredVariables.add(variable)
                }
            }

            is Node.Statement.Assignment -> {
                val variable = declaredVariables.firstOrNull {it.name == node.variable.token.value}
                    ?: throw NotDefined(node.variable.token.value)
                checkExpression(variable.type, node.expression)
            }
            is Node.Function -> checkFunction(node)
            else -> Unit
        }
    }

    private fun checkFunction(function: Node.Function) {
        val returnType = function.returnType
        val returnsInBody = mutableListOf<Node.Statement.Return>()
        function.body.statements.forEach { statement ->
            if(statement is Node.Statement.Return) returnsInBody.add(statement)
        }
        if (returnsInBody.isEmpty()) throw MessageException("Function $function has no return statements")
        returnsInBody.forEach {
            checkExpression(returnType, it.expression)
        }

        for(statement in function.body.statements) {
            checkStatement(statement)
        }

        declaredFunctions.add(function)
    }

    private fun checkStatement(statement: Node.Statement) {
        TODO("Not yet implemented")
    }

    private fun checkFunctionCall(type: Node.Type, name: Node.Identifier, arguments: List<Node.Argument>) {
        val function = declaredFunctions.firstOrNull { it.name == name}
            ?: throw NotDefined(name.token.value + "()")
        if (type != function.returnType) throw TypeMismatch(type, function.returnType.typeSpecifier.token.value, type.typeSpecifier.token.position)
        checkArguments(function.parameters, arguments)

    }
    private fun checkArguments(parameters: List<Node.Parameter>, arguments: List<Node.Argument>) {
        if (parameters.size != arguments.size) throw MessageException("arguments are incorrect")
        for (i in parameters.indices) {
            checkExpression(parameters[i].type, arguments[i].expression)
        }
    }
    private fun checkExpression(type: Node.Type, expression: Node.Expression) {
        when(expression) {
            is Node.Expression.Constant -> checkConstant(type, expression)
            is Node.Expression.Variable -> checkVariable(type, expression)
            is Node.Expression.FunctionCall -> checkFunctionCall(type, expression.name, expression.arguments)
            is Node.Expression.Prefix -> checkVariable(type, expression.variable)
            is Node.Expression.Postfix -> checkVariable(type, expression.variable)
            is Node.Expression.Binary -> checkBinary(type, expression)
            else -> Unit
        }
    }

    private fun checkBoolExpression(type: Node.Type, expression: Node.Expression.Binary) {
        when (expression) {
            is Node.Expression.Binary.Logical, is Node.Expression.Binary.Compare -> {
                if (type.typeSpecifier.token.value != "bool") throw TypeMismatch(type, expression.operator.token.type.toString(), expression.operator.token.position)
            }
            else -> Unit
        }
    }
    private fun checkBinary(type: Node.Type, expression: Node.Expression.Binary) {
        checkExpression(type, expression.left)
        checkExpression(type, expression.right)
        checkBoolExpression(type, expression)

    }

    private fun checkConstant(type: Node.Type, constant: Node.Expression.Constant) {
        if (type.typeSpecifier.token.value == "int" && constant.token.type != TokenType.Constant.Int) {
            throw TypeMismatch(type, constant.token.type.toString(), constant.token.position)
        }
        if (type.typeSpecifier.token.value == "long" && constant.token.type != TokenType.Constant.Int) {
            throw TypeMismatch(type, constant.token.type.toString(), constant.token.position)
        }
        if (type.typeSpecifier.token.value == "double" && constant.token.type != TokenType.Constant.Double) {
            throw TypeMismatch(type, constant.token.type.toString(), constant.token.position)
        }
        if (type.typeSpecifier.token.value == "float" && constant.token.type != TokenType.Constant.Float) {
            throw TypeMismatch(type, constant.token.type.toString(), constant.token.position)
        }
        if (type.typeSpecifier.token.value == "char" && constant.token.type != TokenType.Constant.Symbol) {
            throw TypeMismatch(type, constant.token.type.toString(), constant.token.position)
        }
    }
    private fun checkVariable(type: Node.Type, variable: Node.Expression.Variable) {
        val another = declaredVariables.firstOrNull{ v -> v.name == variable.token.value }
            ?: throw NotDefined(variable.token.value)
        if (another.value == null) throw NotInitialized(another)
        if (type.typeSpecifier.token.value != another.type.typeSpecifier.token.value) {
            throw TypeMismatch(type, variable.token.type.toString(), variable.token.position)
        }
    }
}
