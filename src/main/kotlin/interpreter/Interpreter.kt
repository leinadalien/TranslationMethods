package interpreter

import lexis.token.TokenType
import semantic.Variable
import semantic.exceptions.MessageException
import semantic.exceptions.NotDefined
import semantic.exceptions.NotInitialized
import semantic.exceptions.TypeMismatch
import syntax.SyntaxAnalyzer
import syntax.ast.Node
import java.io.*

class Interpreter(private val program: Node.Program) {
    private val fileName = "input.c"
    private val declaredVariables = mutableListOf<MutableList<Variable>>()
    private val declaredFunctions = mutableListOf<Node.Function>()

    constructor(fileName: String) : this(SyntaxAnalyzer(fileName).parse())

    private fun executeProgram(program: Node.Program) {
        for (function in program.functions) {
            executeFunction(function)
        }
    }

    private fun executeFunction(function: Node.Function) {
        val returnType = function.returnType
        val returnsInBody = mutableListOf<Node.Statement.Return>()
        function.body.statements.forEach { statement ->
            if (statement is Node.Statement.Return) returnsInBody.add(statement)
        }
        if (returnsInBody.isEmpty()) throw MessageException("Function $function has no return statements")
        val functionVariables = mutableListOf<Variable>()
        declaredVariables.add(functionVariables)
        function.parameters.forEach { parameter ->
            functionVariables.add(Variable.functionVariable(parameter.type, parameter.name.token.value))
        }
        for (statement in function.body.statements) {
            executeStatement(statement)
        }
        returnsInBody.forEach {
            executeExpression(returnType, it.expression)
        }
        declaredFunctions.add(function)
        declaredVariables.remove(functionVariables)
    }

    private fun executeStatement(statement: Node.Statement) {
        when (statement) {
            is Node.Statement.Declaration -> executeDeclaration(statement)
            is Node.Statement.Assignment -> executeAssignment(statement)
            is Node.Statement.For -> executeForStatement(statement)
            is Node.Statement.ExpressionStatement -> executeExpressionStatement(statement)
            else -> Unit
        }
    }
    private fun executeExpressionStatement(statement: Node.Statement.ExpressionStatement) {
        executeExpression(null, statement.expression)
    }

    private fun executeAssignment(statement: Node.Statement.Assignment) {
        val variable = findVariable(statement.variable.token.value)
        executeExpression(variable.type, statement.expression)
    }


    private fun executeDeclaration(declaration: Node.Statement.Declaration) {
        declaration.declarators.forEach { declaredVariables.last().addAll(executeDeclarator(declaration.type, it)) }
    }

    private fun executeDeclarator(type: Node.Type, declarator: Node.Declarator) : List<Variable> {
        val variables = mutableListOf<Variable>()
        when (declarator) {
            is Node.Declarator.Variable ->  {
                variables.add(defineVariable(type, declarator.name.token.value))
                declarator.initializer?.let {
                    when(it) {
                        is Node.Initializer.Variable -> executeExpression(type, it.expression)
                        else -> Unit
                    }
                }
            }
            else -> Unit
        }
        return  variables
    }

    private fun executeFunctionCall(type: Node.Type?, name: Node.Identifier, arguments: List<Node.Argument>) {
        val function = declaredFunctions.firstOrNull { it.name == name }
            ?: throw NotDefined(name.token.value + "()")
        if ( (type == null) || type != function.returnType) throw TypeMismatch(
            type,
            function.returnType?.typeSpecifier?.token?.value ?: "void",
            type?.typeSpecifier?.token?.position
        )
        executeArguments(function.parameters, arguments)

    }

    private fun executeArguments(parameters: List<Node.Parameter>, arguments: List<Node.Argument>) {
        if (parameters.size != arguments.size) throw MessageException("arguments are incorrect")
        for (i in parameters.indices) {
            executeExpression(parameters[i].type, arguments[i].expression)
        }
    }

    private fun executeExpression(type: Node.Type?, expression: Node.Expression) {
        when (expression) {
            is Node.Expression.Constant -> executeConstant(type!!, expression)
            is Node.Expression.Variable -> executeVariable(type!!, expression)
            is Node.Expression.FunctionCall -> executeFunctionCall(type, expression.name, expression.arguments)
            is Node.Expression.Prefix -> executePrefExpression(type, expression)
            is Node.Expression.Postfix -> executePostExpression(type, expression)
            is Node.Expression.Binary -> executeBinary(type!!, expression)
            else -> Unit
        }
    }
    private fun executePostExpression(type: Node.Type?, expression: Node.Expression.Postfix) {
        type?.let {
            executeVariable(it, expression.variable)
        }
    }
    private fun executePrefExpression(type: Node.Type?, expression: Node.Expression.Prefix) {
        type?.let {
            executeVariable(it, expression.variable)
        }
    }

    private fun executeBoolExpression(type: Node.Type, expression: Node.Expression.Binary) {
        when (expression) {
            is Node.Expression.Binary.Logical, is Node.Expression.Binary.Compare -> {
                if (type.typeSpecifier.token.value != "bool") throw TypeMismatch(
                    type,
                    expression.operator.token.type.toString(),
                    expression.operator.token.position
                )
            }

            else -> Unit
        }
    }

    private fun executeBinary(type: Node.Type, expression: Node.Expression.Binary) {
        executeExpression(type, expression.left)
        executeExpression(type, expression.right)
        executeBoolExpression(type, expression)

    }

    private fun executeConstant(type: Node.Type, constant: Node.Expression.Constant) {
        if (type.typeSpecifier.token.value == "int" && constant.token.type != TokenType.Constant.Int) {
            throw TypeMismatch(type, constant.token.value, constant.token.position)
        }
        if (type.typeSpecifier.token.value == "long" && constant.token.type != TokenType.Constant.Int) {
            throw TypeMismatch(type, constant.token.value, constant.token.position)
        }
        if (type.typeSpecifier.token.value == "double" && constant.token.type != TokenType.Constant.Double) {
            throw TypeMismatch(type, constant.token.value, constant.token.position)
        }
        if (type.typeSpecifier.token.value == "float" && constant.token.type != TokenType.Constant.Float) {
            throw TypeMismatch(type, constant.token.value, constant.token.position)
        }
        if (type.typeSpecifier.token.value == "char" && constant.token.type != TokenType.Constant.Symbol) {
            throw TypeMismatch(type, constant.token.value, constant.token.position)
        }
    }

    private fun executeForStatement(forStatement: Node.Statement.For) {
        val forVariables = mutableListOf<Variable>()
        declaredVariables.add(forVariables)
        forStatement.apply {
            initialization.declarators.forEach {
                executeDeclarator(forStatement.initialization.type, it)
                when(it) {
                    is Node.Declarator.Variable -> forVariables.add(defineVariable(forStatement.initialization.type, it.name.token.value))
                    is Node.Declarator.Pointer -> forVariables.add(defineVariable(forStatement.initialization.type, it.variable.token.value))
                }
            }
            executeExpression(null, increase)
            body.statements.forEach {executeStatement(it)}
        }
        declaredVariables.remove(forVariables)




    }
    private fun executeVariable(type: Node.Type, variable: Node.Expression.Variable, needInitializer: Boolean = false) {
        var another: Variable? = null
        declaredVariables.forEach { nested ->
            nested.firstOrNull { v -> v.name == variable.token.value }?.let { another = it }
        }
        another?.let {
            if (it.value == null && needInitializer) throw NotInitialized(it)
            if (type.typeSpecifier.token.value != it.type.typeSpecifier.token.value) {
                throw TypeMismatch(type, variable.token.type.toString(), variable.token.position)
            }
        } ?: apply { throw NotDefined(variable.token.value) }

    }

    private fun findVariable(name: String): Variable {
        var result: Variable? = null
        declaredVariables.forEach { nested ->
            nested.firstOrNull { v -> v.name == name }?.let {
                if (result == null) result = it
                else throw MessageException("Variable $name is already defined")
            }
        }
        result?.let { return it }
        throw NotDefined(name)
    }

    private fun defineVariable(type: Node.Type, name: String, ) : Variable {
        var result: Variable? = null
        declaredVariables.forEach { nested ->
            nested.firstOrNull { v -> v.name == name }?.let {
                throw MessageException("Variable $name is already defined") }
        }
        return Variable(type, name, null)
    }
    fun execute(){
        executeProgram(program)
    }
}