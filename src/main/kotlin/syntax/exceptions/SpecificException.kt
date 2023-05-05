package syntax.exceptions


class SpecificException(var exception: SyntaxException, var position: Int): SyntaxException()