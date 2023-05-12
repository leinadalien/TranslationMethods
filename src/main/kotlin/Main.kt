
import interpreter.Interpreter
import semantic.SemanticAnalyzer
import syntax.SyntaxAnalyzer

fun main() {
    val syntaxAnalyzer = SyntaxAnalyzer("input.c")
    val program = syntaxAnalyzer.parse()
    SemanticAnalyzer(program).checkForMistakes()
    val interpreter = Interpreter(program)
    interpreter.execute()
}


