package semantic

import lexis.token.TokenType
import semantic.exceptions.MessageException
import semantic.exceptions.NotDefined
import semantic.exceptions.NotInitialized
import semantic.exceptions.TypeMismatch
import syntax.SyntaxAnalyzer
import syntax.ast.Node

class SemanticAnalyzer(private val tree: Node.Program) {
    private val declaredVariables = mutableListOf<MutableList<Variable>>()
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

    private fun checkFunction(function: Node.Function) {
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
            checkStatement(statement)
        }
        returnsInBody.forEach {
            checkExpression(returnType, it.expression)
        }
        declaredFunctions.add(function)
        declaredVariables.remove(functionVariables)
    }

    private fun checkStatement(statement: Node.Statement) {
        when (statement) {
            is Node.Statement.Declaration -> checkDeclaration(statement)
            is Node.Statement.Assignment -> checkAssignment(statement)
            is Node.Statement.For -> checkForStatement(statement)
            is Node.Statement.ExpressionStatement -> checkExpressionStatement(statement)
            else -> Unit
        }
    }
    private fun checkExpressionStatement(statement: Node.Statement.ExpressionStatement) {
        checkExpression(null, statement.expression)
    }

    private fun checkAssignment(statement: Node.Statement.Assignment) {
        val variable = findVariable(statement.variable.token.value)
        checkExpression(variable.type, statement.expression)
    }


    private fun checkDeclaration(declaration: Node.Statement.Declaration) {
        declaration.declarators.forEach { declaredVariables.last().addAll(checkDeclarator(declaration.type, it)) }

    }

    private fun checkDeclarator(type: Node.Type, declarator: Node.Declarator) : List<Variable> {
        val variables = mutableListOf<Variable>()
        when (declarator) {
            is Node.Declarator.Variable ->  {
                variables.add(defineVariable(type, declarator.name.token.value))
                declarator.initializer?.let {
                    when(it) {
                        is Node.Initializer.Variable -> checkExpression(type, it.expression)
                        else -> Unit
                    }
                }
            }
            else -> Unit
        }
        return  variables
    }

    private fun checkFunctionCall(type: Node.Type?, name: Node.Identifier, arguments: List<Node.Argument>) {
        val function = declaredFunctions.firstOrNull { it.name == name }
            ?: throw NotDefined(name.token.value + "()")
        if ( (type == null) || type != function.returnType) throw TypeMismatch(
            type,
            function.returnType?.typeSpecifier?.token?.value ?: "void",
            type?.typeSpecifier?.token?.position
        )
        checkArguments(function.parameters, arguments)

    }

    private fun checkArguments(parameters: List<Node.Parameter>, arguments: List<Node.Argument>) {
        if (parameters.size != arguments.size) throw MessageException("arguments are incorrect")
        for (i in parameters.indices) {
            checkExpression(parameters[i].type, arguments[i].expression)
        }
    }

    private fun checkExpression(type: Node.Type?, expression: Node.Expression) {
        when (expression) {
            is Node.Expression.Constant -> checkConstant(type!!, expression)
            is Node.Expression.Variable -> checkVariable(type!!, expression)
            is Node.Expression.FunctionCall -> checkFunctionCall(type, expression.name, expression.arguments)
            is Node.Expression.Prefix -> checkPrefExpression(type, expression)
            is Node.Expression.Postfix -> checkPostExpression(type, expression)
            is Node.Expression.Binary -> checkBinary(type!!, expression)
            else -> Unit
        }
    }
    private fun checkPostExpression(type: Node.Type?, expression: Node.Expression.Postfix) {
        type?.let {
            checkVariable(it, expression.variable)
        }
    }
    private fun checkPrefExpression(type: Node.Type?, expression: Node.Expression.Prefix) {
        type?.let {
            checkVariable(it, expression.variable)
        }
    }

    private fun checkBoolExpression(type: Node.Type, expression: Node.Expression.Binary) {
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

    private fun checkBinary(type: Node.Type, expression: Node.Expression.Binary) {
        checkExpression(type, expression.left)
        checkExpression(type, expression.right)
        checkBoolExpression(type, expression)

    }

    private fun checkConstant(type: Node.Type, constant: Node.Expression.Constant) {
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

    private fun checkForStatement(forStatement: Node.Statement.For) {
        val forVariables = mutableListOf<Variable>()
        declaredVariables.add(forVariables)
        forStatement.apply {
            initialization.declarators.forEach {
                checkDeclarator(forStatement.initialization.type, it)
                when(it) {
                    is Node.Declarator.Variable -> forVariables.add(defineVariable(forStatement.initialization.type, it.name.token.value))
                    is Node.Declarator.Pointer -> forVariables.add(defineVariable(forStatement.initialization.type, it.variable.token.value))
                }
            }
            checkExpression(null, increase)
            body.statements.forEach {checkStatement(it)}
        }
        declaredVariables.remove(forVariables)




    }
    private fun checkVariable(type: Node.Type, variable: Node.Expression.Variable, needInitializer: Boolean = false) {
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
}
