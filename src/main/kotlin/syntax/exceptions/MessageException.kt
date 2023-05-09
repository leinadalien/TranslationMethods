package syntax.exceptions

class MessageException(val msg: String, tokenPosition: Int) : SyntaxException(tokenPosition) {
    override val message: String
        get() = msg
}