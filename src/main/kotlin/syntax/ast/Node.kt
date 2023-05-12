package syntax.ast

import lexis.token.Token
import lexis.token.TokenType
import kotlin.text.StringBuilder

sealed class Node {
    abstract fun printToConsole(indent: String = "")
    data class Program(val functions : List<Function>) : Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Program:")
            functions.forEach { it.printToConsole("$indent  ")}
        }
    }

    data class Function(
        val returnType: Type?,
        val name: Identifier,
        val parameters: List<Parameter>,
        val body: Statement.Compound,
    ) : Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Function:")
            returnType?.let {
                it.printToConsole("$indent· ")
            }?: apply { println("$indent· void") }
            name.printToConsole("$indent· ")
            if (parameters.isNotEmpty()) {
                println("$indent  Parameters:")
                parameters.forEach {
                    it.printToConsole("$indent· · ")
                }
            }
            println("$indent· Body:")
            body.printToConsole("$indent· · ")
        }

        override fun toString(): String {
            val parametersBuilder = StringBuilder()
            for(parameter in parameters) {
                parametersBuilder.append(parameter.type, ' ', parameter.name.token.value)
                if (parameter != parameters.last()) parametersBuilder.append(", ")
            }
            return "${name.token.value}(${parametersBuilder})"
        }
    }

    data class Identifier(val token: Token) : Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Name: ${token.value}")
        }

        override fun toString(): String {
            return token.value
        }
    }
    //tokenized is temp
    data class Tokenized(val token: Token) : Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Token")
            println("$indent· type: ${token.type}")
            println("$indent· value: ${token.value}")
        }

        override fun toString(): String {
            return token.value
        }
    }
    data class Operator(val token: Token) : Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Operator: ${token.value}")
        }
    }

    data class Parameter(
        val type: Type,
        val name: Identifier
    ) : Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Parameter:")
            type.printToConsole("$indent· ")
            name.printToConsole("$indent· ")
        }
    }

    data class Argument(
        val expression: Expression
    ): Node() {
        override fun printToConsole(indent: String) {
            println("${indent}Argument:")
            expression.printToConsole("$indent· ")
        }
    }

    sealed class Declarator: Node() {
        data class Variable(
            val name: Identifier,
            val initializer: Initializer?
        ) : Declarator() {
            override fun printToConsole(indent: String) {
                println("${indent}Variable declarator:")
                name.printToConsole("$indent· ")
                initializer?.printToConsole("$indent· ")
            }
        }
        data class Pointer(
            val variable: Expression.Variable,
            val initializer: Initializer?
        ) : Declarator() {
            override fun printToConsole(indent: String) {
                println("${indent}Pointer declarator:")
                variable.printToConsole("$indent· ")
                initializer?.printToConsole("$indent· ")
            }
        }
    }



    sealed class Initializer : Node() {
        data class Variable(
            val expression: Expression
        ) : Initializer() {
            override fun printToConsole(indent: String) {
                println("${indent}Variable initializer:")
                expression.printToConsole("$indent· ")
            }
        }
        data class Pointer(
            val variable: Expression.Variable
        ) : Initializer() {
            override fun printToConsole(indent: String) {
                println("${indent}Pointer initializer:")
                variable.printToConsole("$indent· ")
            }
        }

    }



    sealed class Statement: Node() {
        data class Declaration(
            val type: Type,
            val declarators: List<Declarator>
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Declaration:")
                type.printToConsole("$indent· ")
                println("$indent· Declarators:")
                declarators.forEach { it.printToConsole("$indent· · ") }
            }
        }

        data class TypedefDeclaration(
            val type: Type,
            val name: Identifier
        ): Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Typedef declaration:")
                type.printToConsole("$indent· ")
                name.printToConsole("$indent· ")
            }
        }

        data class Compound(
            val statements: List<Statement>
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Compound statement:")
                statements.forEach { it.printToConsole("$indent· ") }
            }
        }

        data class If(
            val condition: Expression.Conditional,
            val trueBranch: Compound,
            val falseBranch: Compound?
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}If statement:")
                condition.printToConsole("$indent· ")
                println("${indent}· True branch:")
                trueBranch.printToConsole("$indent· · ")
                println("${indent}· False branch:")
                falseBranch?.printToConsole("$indent· · ")
            }
        }

        data class Switch(
            val expression: Expression,
            val cases: List<Case>,
            val default: Default?
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Switch statement:")
                expression.printToConsole("$indent· ")
                cases.forEach { it.printToConsole("$indent· ") }
                default?.printToConsole("$indent· ")
            }
        }

        data class Case(
            val variant: Expression.Constant,
            val statements: List<Statement>
        ): Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Case statement:")
                variant.printToConsole("$indent· ")
                statements.forEach { it.printToConsole("$indent· ") }
            }
        }

        data class Default(
            val statements: List<Statement>
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Default statement:")
                statements.forEach { it.printToConsole("$indent· ") }
            }
        }

        data class For(
            val initialization: Declaration,
            val condition: Expression.Conditional,
            val increase: Expression,
            val body: Compound
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}For statement:")
                initialization.printToConsole("$indent· ")
                condition.printToConsole("$indent· ")
                increase.printToConsole("$indent· ")
                body.printToConsole("$indent· ")
            }
        }

        data class While(
            val condition: Expression.Conditional,
            val body: Compound
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}While statement:")
                condition.printToConsole("$indent· ")
                body.printToConsole("$indent· ")
            }
        }

        data class DoWhile(
            val body: Compound,
            val condition: Expression.Conditional
        ): Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Do while statement:")
                body.printToConsole("$indent· ")
                condition.printToConsole("$indent· ")
            }
        }

        data class Return(
            val expression: Expression
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Return statement:")
                expression.printToConsole("$indent· ")
            }
        }
        object Break: Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Break statement")
            }
        }

        data class ExpressionStatement(
            val expression: Expression
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Expression statement:")
                expression.printToConsole("$indent· ")
            }
        }
        data class Assignment(
            val variable: Expression.Variable,
            val expression: Expression,
        ) : Statement() {
            override fun printToConsole(indent: String) {
                println("${indent}Assignment statement:")
                variable.printToConsole("$indent· ")
                expression.printToConsole("$indent· ")
            }
        }
    }

    sealed class Expression : Node() {

        data class Constant(
            val token: Token
        ) : Expression() {
            override fun printToConsole(indent: String) {
                print("${indent}Constant: ")
                println(token.value)
            }
        }

        data class Variable(
            val token: Token
        ) : Expression() {
            override fun printToConsole(indent: String) {
                print("${indent}Variable: ")
                println(token.value)
            }
        }
        data class Conditional(
            val expression: Expression
        ) : Expression() {
            override fun printToConsole(indent: String) {
                println("${indent}Condition: ")
                expression.printToConsole("$indent· ")
            }
        }
        sealed class Binary(
            val left: Expression,
            val operator: Operator,
            val right: Expression
        ) : Expression() {
            class Compare(
                left: Expression,
                operator: Operator,
                right: Expression
            ) : Binary(left, operator, right) {
                override fun printToConsole(indent: String) {
                    println("${indent}Compare expression:")
                    left.printToConsole("$indent· ")
                    operator.printToConsole("$indent· ")
                    right.printToConsole("$indent· ")
                }
            }

            class Logical(
                left: Expression,
                operator: Operator,
                right: Expression
                //correct
            ) : Binary(left, operator, right) {
                override fun printToConsole(indent: String) {
                    println("${indent}Logical expression:")
                    left.printToConsole("$indent· ")
                    operator.printToConsole("$indent· ")
                    right.printToConsole("$indent· ")
                }
            }
            class Multiply(
                left: Expression,
                operator: Operator,
                right: Expression
                //correct
            ) : Binary(left, operator, right) {
                override fun printToConsole(indent: String) {
                    println("${indent}Multiply expression:")
                    left.printToConsole("$indent· ")
                    operator.printToConsole("$indent· ")
                    right.printToConsole("$indent· ")
                }
            }
            class Addictive(
                left: Expression,
                operator: Operator,
                right: Expression
                //correct
            ) : Binary(left, operator, right) {
                override fun printToConsole(indent: String) {
                    println("${indent}Addictive expression:")
                    left.printToConsole("$indent· ")
                    operator.printToConsole("$indent· ")
                    right.printToConsole("$indent· ")
                }
            }
        }

        sealed class Unary(
            val operator: Operator,
            val expression: Expression
        ) : Expression() {
            class Logical(
                operator: Operator,
                expression: Expression
            ) : Unary(operator, expression) {
                override fun printToConsole(indent: String) {
                    println("${indent}Logical expression:")
                    operator.printToConsole("$indent· ")
                    expression.printToConsole("$indent· ")
                }
            }
        }
        data class Postfix(
            val variable: Variable,
            val operator: Operator
        ) : Expression() {
            override fun printToConsole(indent: String) {
                println("${indent}Postfix expression:")
                variable.printToConsole("$indent· ")
                operator.printToConsole("$indent· ")
            }
        }
        data class Prefix(
            val operator: Operator,
            val variable: Variable,
        ) : Expression() {
            override fun printToConsole(indent: String) {
                println("${indent}Prefix expression:")
                operator.printToConsole("$indent· ")
                variable.printToConsole("$indent· ")
            }
        }


        data class FunctionCall(
            val name: Identifier,
            val arguments: List<Argument>
        ) : Expression() {
            override fun printToConsole(indent: String) {
                println("${indent}Function call:")
                name.printToConsole("$indent· ")
                arguments.forEach { it.printToConsole("$indent· ") }
            }
        }
    }


    data class Type(
        val typeModifier: Tokenized?,
        val memoryModifier: Tokenized?,
        val typeSpecifier: Tokenized
    ) : Node() {
        override fun printToConsole(indent: String) {
            print("${indent}Type: ")
            typeModifier?.let { printToConsole(it.token.value + ' ') }
            memoryModifier?.let { printToConsole(it.token.value + ' ') }
            println(typeSpecifier.token.value)
        }

        override fun toString(): String {
            val builder = StringBuilder()
            typeModifier?.let { builder.append(it, ' ') }
            memoryModifier?.let { builder.append(it, ' ') }
            builder.append(typeSpecifier)
            return builder.toString()
        }
    }
}