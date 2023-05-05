package semantic.exceptions

import semantic.Variable

class NotInitialized(val variable: Variable) : SemanticException() {
    override val message: String
        get() = "Variable ${variable.name} is not initialized"
}