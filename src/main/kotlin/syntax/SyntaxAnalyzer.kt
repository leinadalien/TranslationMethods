package syntax

import lexis.Analyzer
import lexis.token.Token
import lexis.token.TokenType
import syntax.ast.Node
import syntax.exceptions.*

class SyntaxAnalyzer(private val tokens: List<Token>) {
    private var currentPosition = 0
    private fun getNextToken() = tokens[currentPosition++]
    private fun peekNextToken() = tokens[currentPosition]
    private fun checkNextTokenOnValue(value: String) = peekNextToken().value == value
    private inline fun <reified Type : TokenType> checkNextToken(value: String? = null): Boolean {
        when (peekNextToken().type) {
            is Type -> {
                value?.let {
                    return peekNextToken().value == value
                }
                return true
            }

            else -> return false
        }
    }

    constructor(fileName: String) : this(Analyzer(fileName).tokens)

    private fun consumeNextTokenOnValue(value: String): Token {
        val token = getNextToken()
        if (token.value != value) throw UnexpectedTokenException(value, token)
        return token
    }

    private inline fun <reified Type : TokenType> consumeNextToken(expected: String = Type::class.simpleName.toString()): Token {
        val token = getNextToken()
        if (token.type !is Type) throw UnexpectedTokenException(expected, token)
        return token
    }

    fun parse(): Node {
        val funcDefinitions = mutableListOf<Node.Function>()
        while (currentPosition < tokens.size) {
            try {
                val funcDefinition = parseFunctionDefinition()
                funcDefinitions.add(funcDefinition)
            } catch (ex: SpecificException) {
                throw ex.exception
            }
        }
        return Node.Program(funcDefinitions)
    }

    private fun parseTokenOnValue(value: String): Node.Tokenized {
        return Node.Tokenized(consumeNextTokenOnValue(value))
    }

    private fun parseFirstOf(vararg parseFunctions: () -> Node): Node {
        val savedPosition = currentPosition
        var exception = SpecificException(MessageException(""), 0)
        for (function in parseFunctions) {
            try {
                return function.invoke()
            } catch (ex: SyntaxException) {
                if (ex is SpecificException) {
                    if (ex.position >= exception.position) exception = ex
                } else if (currentPosition >= exception.position) exception = SpecificException(ex, currentPosition)
                currentPosition = savedPosition
            }
        }
        throw exception
    }

    private fun tryParse(parseFunction: () -> Node): Node? {
        var result: Node? = null
        val savedPosition = currentPosition
        try {
            result = parseFunction.invoke()
        } catch (_: SyntaxException) {
            currentPosition = savedPosition
        }
        return result
    }

    private fun parseFunctionDefinition(): Node.Function {
        val type = parseTypeSpecifier()
        val name = Node.Identifier(consumeNextToken<TokenType.Identifier>())
        val parameters = parseParameterList()
        val body = parseCompoundStatement()
        return Node.Function(type, name, parameters, body)
    }

    private fun parseParameterList(): List<Node.Parameter> {
        val parameters = mutableListOf<Node.Parameter>()
        consumeNextToken<TokenType.Punctuation.Paren.Left>()

        while (!checkNextToken<TokenType.Punctuation.Paren.Right>()) {
            val parameter = parseParameter()
            parameters.add(parameter)
            if (!checkNextToken<TokenType.Punctuation.Paren.Right>()) {
                consumeNextToken<TokenType.Punctuation.Comma>()
            }
        }
        getNextToken()
        return parameters
    }

    private fun parseParameter(): Node.Parameter {
        val type = parseTypeSpecifier()
        val name = Node.Identifier(consumeNextToken<TokenType.Identifier>())
        return Node.Parameter(type, name)
    }

    private fun parseCompoundStatement(): Node.Statement.Compound {
        val statements = mutableListOf<Node.Statement>()
        consumeNextToken<TokenType.Punctuation.Brace.Left>()
        var statement = tryParse { parseStatement() }
        while (statement != null) {
            statements.add(statement as Node.Statement)
            statement = tryParse { parseStatement() }
        }
        consumeNextToken<TokenType.Punctuation.Brace.Right>("}")
        return Node.Statement.Compound(statements)
    }

    private fun parseTypedefDeclaration(): Node {
        consumeNextTokenOnValue("typedef")
        val type = parseTypeSpecifier()
        val name = Node.Identifier(consumeNextToken<TokenType.Identifier>())
        return Node.Statement.TypedefDeclaration(type, name)
    }

    private fun parseTypeSpecifier(): Node.Type {
        val typeModifier = tryParse {
            parseTypeModifier()
        }
        var typeSpecifier: Node? = null
        var memoryModifier = tryParse {
            parseMemoryModifier()
        }
        memoryModifier?.let {
            typeSpecifier = tryParse {
                parseType()
            }
            typeSpecifier?.let { t ->
                val type = t as Node.Tokenized
                val modifier = it as Node.Tokenized
                if (modifier.token.value == "short" && type.token.value != "int") throw TypeSpecificException("After 'short' can be only 'int'")
                if (modifier.token.value == "long") {
                    when (type.token.value) {
                        "float", "char" -> throw TypeSpecificException("'long' and '${type.token.value}' can't be in type specifier")
                    }
                }
            } ?: {
                typeSpecifier = memoryModifier
                memoryModifier = null
            }
        } ?: apply {
            typeSpecifier = parseType()
        }
        return Node.Type(
            if (typeModifier != null) typeModifier as Node.Tokenized else null,
            if (memoryModifier != null) memoryModifier as Node.Tokenized else null,
            typeSpecifier as Node.Tokenized
        )
    }

    private fun parseMemoryModifier(): Node {
        return parseFirstOf(
            { parseTokenOnValue("short") },
            { parseTokenOnValue("long") }
        )
    }

    private fun parseType(): Node {
        return parseFirstOf(
            { parseTokenOnValue("float") },
            { parseTokenOnValue("double") },
            { parseTokenOnValue("char") },
            { parseTokenOnValue("int") },
            { parseTokenOnValue("long") },
        )
    }

    private fun parseTypeModifier(): Node {
        return parseFirstOf(
            { parseTokenOnValue("unsigned") },
            { parseTokenOnValue("signed") }
        )
    }

    private fun parseStatement(): Node.Statement {
        return parseFirstOf(
            ::parseTypedefDeclaration,
            ::parseCompoundStatement,
            ::parseIfStatement,
            ::parseSwitchStatement,
            ::parseForStatement,
            ::parseWhileStatement,
            ::parseDoWhileStatement,
            ::parseReturnStatement,
            ::parseDeclaration,
            //::parsePrimaryExpression,
            ::parseExpressionStatement
        ) as Node.Statement
    }

    private fun parseIfStatement(): Node {
        consumeNextTokenOnValue("if")
        val condition = parseCondition()
        val trueBranch = parseCompoundStatement()
        val falseBranch = parseElseStatement()
        return Node.Statement.If(condition, trueBranch, falseBranch)
    }

    private fun parseElseStatement(): Node.Statement.Compound {
        consumeNextTokenOnValue("else")
        return parseCompoundStatement()
    }

    private fun parseConditionExpression(): Node.Expression.Conditional {
        when(val expression = parseExpression()) {
            is Node.Expression.Binary.Logical, is Node.Expression.Binary.Compare, is Node.Expression.Constant -> return Node.Expression.Conditional(expression)
            else -> throw MessageException("Expected condition in for")
        }
    }

    private fun parseCondition(): Node.Expression.Conditional {
        consumeNextToken<TokenType.Punctuation.Paren.Left>()
        val condition = parseConditionExpression()
        consumeNextToken<TokenType.Punctuation.Paren.Right>()
        return condition
    }

    private fun parseLogicalExpression(): Node {
        return parseFirstOf(
            ::parseCompareExpression,
            ::parseFunctionCall,
            ::parseBoolConstant,
            ::parseInverseLogicalExpression
        )
    }

    private fun parseBoolConstant(): Node.Expression.Constant {
        return Node.Expression.Constant(consumeNextToken<TokenType.Constant.Bool>())
    }

    private fun parseInverseLogicalExpression(): Node.Expression.Unary.Logical {
        val operator = consumeNextToken<TokenType.Operator.Not>()
        val expression = parseLogicalExpression() as Node.Expression
        return Node.Expression.Unary.Logical(Node.Operator(operator), expression)
    }

    private fun parseCompareExpression(): Node.Expression.Binary.Compare {
        val left = parseExpression()
        val operator = Node.Operator(consumeNextToken<TokenType.Operator.Binary.Comparison>())
        val right = parseExpression()
        return Node.Expression.Binary.Compare(left, operator, right)
    }

    private fun parseFunctionCall(): Node.Expression.FunctionCall {
        val name = Node.Identifier(consumeNextToken<TokenType.Identifier>())
        val arguments = parseArgumentList()
        return Node.Expression.FunctionCall(name, arguments)
    }

    private fun parseArgumentList(): List<Node.Argument> {
        val arguments = mutableListOf<Node.Argument>()
        consumeNextToken<TokenType.Punctuation.Paren.Left>()
        tryParse { parseArgument() }?.let {
            arguments.add(it as Node.Argument)
            var comma = tryParse { Node.Tokenized(consumeNextToken<TokenType.Punctuation.Comma>()) }
            while (comma != null) {
                arguments.add(parseArgument())
                comma = tryParse { Node.Tokenized(consumeNextToken<TokenType.Punctuation.Comma>()) }
            }
        }
        consumeNextToken<TokenType.Punctuation.Paren.Right>()
        return arguments
    }

    private fun parseArgument(): Node.Argument {
        return Node.Argument(parseExpression())
    }

    private fun parseExpression(): Node.Expression {
        var left = parsePrimaryExpression()
        while (checkNextToken<TokenType.Operator.Binary>()) {
            left = parseBinaryExpression(left)
        }
        return left
    }
    private fun parseParentedExpression() : Node.Expression {
        consumeNextToken<TokenType.Punctuation.Paren.Left>("(")
        val expression = parseExpression()
        consumeNextToken<TokenType.Punctuation.Paren.Right>(")")
        return expression
    }
    private fun parseBinaryExpression(left: Node.Expression): Node.Expression {
        val operator = consumeNextToken<TokenType.Operator.Binary>()
        var right = parsePrimaryExpression()
        val priority = getBinaryOperatorPriority(operator.value)
        if (checkNextToken<TokenType.Operator.Binary>() && getBinaryOperatorPriority(peekNextToken().value) > priority) {
            right = parseBinaryExpression(right)
        }
        return when(operator.type) {
            is TokenType.Operator.Binary.Multiply -> Node.Expression.Binary.Multiply(left, Node.Operator(operator), right)
            is TokenType.Operator.Binary.Logical -> Node.Expression.Binary.Logical(left, Node.Operator(operator), right)
            is TokenType.Operator.Binary.Comparison -> Node.Expression.Binary.Compare(left, Node.Operator(operator), right)
            else  -> Node.Expression.Binary.Addictive(left, Node.Operator(operator), right)
        }
    }

    private fun getBinaryOperatorPriority(operator: String): Int {
        return when (operator) {
            "*", "/", "%" -> 10
            "+", "-" -> 9
            "<", "<=", ">", ">=" -> 7
            "==", "!=" -> 6
            "&&" -> 2
            "||" -> 1
            else -> throw IllegalArgumentException("Unknown binary operator: $operator")
        }
    }

    private fun parsePostfixExpression() : Node.Expression.Postfix {
        val variable = consumeNextToken<TokenType.Identifier>()
        val operator = consumeNextToken<TokenType.Operator.IncDec>()
        return Node.Expression.Postfix(Node.Expression.Variable(variable), Node.Operator(operator))
    }
    private fun parsePrefixExpression() : Node.Expression.Prefix {
        val operator = consumeNextToken<TokenType.Operator.IncDec>()
        val variable = consumeNextToken<TokenType.Identifier>()
        return Node.Expression.Prefix(Node.Operator(operator), Node.Expression.Variable(variable))
    }
    private fun parseSwitchStatement(): Node.Statement.Switch {
        consumeNextTokenOnValue("switch")
        consumeNextToken<TokenType.Punctuation.Paren.Left>()
        val expression = parseExpression()
        consumeNextToken<TokenType.Punctuation.Paren.Right>()
        consumeNextToken<TokenType.Punctuation.Brace.Left>()
        val variants = mutableListOf<Node>()
        var variant: Node? = parseFirstOf(
            ::parseCaseStatement,
            ::parseDefaultStatement
        )
        while (variant != null) {
            variants.add(
                variant
            )
            variant = tryParse {
                parseFirstOf(
                    ::parseCaseStatement,
                    ::parseDefaultStatement
                )
            }
        }
        consumeNextToken<TokenType.Punctuation.Brace.Right>()
        var default: Node.Statement.Default? = null
        val cases = mutableListOf<Node.Statement.Case>()
        for (variant in variants) {
            if (variant is Node.Statement.Default) {
                if (default == null) default = variant
                else throw MessageException("Switch statement must contain only one 'default'")
            } else {
                cases.add(variant as Node.Statement.Case)
            }
        }
        return Node.Statement.Switch(expression, cases, default)
    }

    private fun parseDefaultStatement(): Node.Statement.Default {
        consumeNextTokenOnValue("default")
        consumeNextToken<TokenType.Punctuation.Colon>()
        val statements = mutableListOf<Node.Statement>()
        while (!checkNextTokenOnValue("case")
            && !checkNextToken<TokenType.Punctuation.Brace.Right>()
        ) {
            statements.add(
                parseFirstOf(
                    ::parseStatement,
                    ::parseBreakStatement
                ) as Node.Statement
            )
        }
        return Node.Statement.Default(statements)
    }

    private fun parseCaseStatement(): Node.Statement.Case {
        consumeNextTokenOnValue("case")
        val variant = Node.Expression.Constant(consumeNextToken<TokenType.Constant>())
        consumeNextToken<TokenType.Punctuation.Colon>()
        val statements = mutableListOf<Node.Statement>()
        while (!checkNextTokenOnValue("case")
            && !checkNextTokenOnValue("default")
            && !checkNextToken<TokenType.Punctuation.Brace.Right>()
        ) {
            statements.add(
                parseFirstOf(
                    ::parseStatement,
                    ::parseBreakStatement
                ) as Node.Statement
            )
        }
        return Node.Statement.Case(variant, statements)
    }
    private fun parseBreakStatement() : Node.Statement.Break {
        val statement = consumeNextTokenOnValue("break")
        consumeNextToken<TokenType.Punctuation.Semicolon>()
        return Node.Statement.Break
    }
    private fun parseForStatement(): Node.Statement {
        consumeNextTokenOnValue("for")
        consumeNextToken<TokenType.Punctuation.Paren.Left>()
        val initialization = parseDeclaration()
        val condition = parseConditionExpression()
        consumeNextToken<TokenType.Punctuation.Semicolon>()
        val increase = parseExpression()
        consumeNextToken<TokenType.Punctuation.Paren.Right>()
        val body = parseCompoundStatement()
        return Node.Statement.For(initialization, condition, increase, body)
    }

    private fun parseWhileStatement(): Node.Statement.While {
        consumeNextTokenOnValue("while")
        val condition = parseCondition()
        val body = parseCompoundStatement()
        return Node.Statement.While(condition, body)
    }

    private fun parseDoWhileStatement(): Node.Statement.DoWhile {
        consumeNextTokenOnValue("do")
        val body = parseCompoundStatement()
        consumeNextTokenOnValue("while")
        val condition = parseCondition()
        consumeNextToken<TokenType.Punctuation.Semicolon>()
        return Node.Statement.DoWhile(body, condition)
    }

    private fun parseReturnStatement(): Node.Statement.Return {
        consumeNextTokenOnValue("return")
        val expression = parseExpression()
        consumeNextToken<TokenType.Punctuation.Semicolon>()
        return Node.Statement.Return(expression)
    }

    private fun parseDeclaration(): Node.Statement.Declaration {
        val type = parseTypeSpecifier()
        val declarators = parseDeclarators()
        consumeNextToken<TokenType.Punctuation.Semicolon>()
        return Node.Statement.Declaration(type, declarators)
    }

    private fun parseDeclarators(): List<Node.Declarator> {
        val declarators = mutableListOf<Node.Declarator>()
        declarators.add(parseDeclarator())
        while (checkNextToken<TokenType.Punctuation.Comma>()) {
            getNextToken()
            declarators.add(parseDeclarator())
        }
        return declarators
    }

    private fun parseInitializer(): Node.Initializer {
        val expression = parseExpression()
        return Node.Initializer(expression)
    }

    private fun parseDeclarator(): Node.Declarator {
        val name = Node.Identifier(consumeNextToken<TokenType.Identifier>())
        var initializer: Node.Initializer? = null
        if (checkNextTokenOnValue("=")) {
            getNextToken()
            initializer = parseInitializer()
        }
        return Node.Declarator(name, initializer)
    }

    private fun parsePrimaryExpression(): Node.Expression {
        return parseFirstOf(
            ::parseConstant,
            ::parseFunctionCall,
            ::parseParentedExpression,
            ::parsePostfixExpression,
            ::parseVariable,
            ::parsePrefixExpression
        ) as Node.Expression
    }

    private fun parseConstant(): Node.Expression.Constant {
        val token = consumeNextToken<TokenType.Constant>()
        return Node.Expression.Constant(token)
    }

    private fun parseVariable(): Node.Expression.Variable {
        val token = consumeNextToken<TokenType.Identifier>()
        return Node.Expression.Variable(token)
    }

    private fun parseExpressionStatement(): Node.Statement {
        val expression = parseExpression()
        consumeNextToken<TokenType.Punctuation.Semicolon>()
        return Node.Statement.ExpressionStatement(expression)
    }
}