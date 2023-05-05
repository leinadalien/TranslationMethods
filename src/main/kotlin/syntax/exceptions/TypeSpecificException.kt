package syntax.exceptions


class TypeSpecificException(private val text: String): SyntaxException() {
    override val message: String
        get() = text
}