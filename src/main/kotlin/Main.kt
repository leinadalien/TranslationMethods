
import syntax.SyntaxAnalyzer

fun main() {
    val sAnalyzer = SyntaxAnalyzer("input.txt")
    val tree = sAnalyzer.parse()
    tree.printToConsole()
}


