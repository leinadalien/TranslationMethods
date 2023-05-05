package syntax.exceptions

class MessageException(val msg: String) : SyntaxException() {
    override val message: String
        get() = msg
}