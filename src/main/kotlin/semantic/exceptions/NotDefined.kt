package semantic.exceptions

class NotDefined(val name: String) : SemanticException() {
    override val message: String
        get() = "The $name is not defined"
}