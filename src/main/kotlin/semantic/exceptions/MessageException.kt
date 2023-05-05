package semantic.exceptions

class MessageException(val msg: String) : SemanticException() {
    override val message: String?
        get() = msg
}