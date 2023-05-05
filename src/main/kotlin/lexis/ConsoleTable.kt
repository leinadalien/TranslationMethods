package lexis

class ConsoleTable(vararg headers: String) {
    private val columnWidths: MutableList<Int> = mutableListOf()
    private val rows: MutableList<List<Any>> = mutableListOf()
    private val headers = headers.toMutableList()
    init {
        headers.forEach { addHeader(it) }
    }

    private fun addHeader(header: String) {
        columnWidths.add(header.length)
        headers.add(header)
    }

    fun addRow(vararg row: Any) {
        rows.add(row.toList())
        row.forEachIndexed { index, value ->
            val valueLength = value.toString().length
            if (valueLength > columnWidths[index]) {
                columnWidths[index] = valueLength
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        val rowSeparator = columnWidths.joinToString("+", "+", "+") { "-".repeat(it + 2) }
        sb.append(rowSeparator).append("\n")

        for ((i, width) in columnWidths.withIndex()) {
            sb.append("| %-${width}s ".format(headers[i]))
        }
        sb.append("|\n").append(rowSeparator).append("\n")

        for (row in rows) {
            for ((i, width) in columnWidths.withIndex()) {
                sb.append("| %-${width}s ".format(row[i]))
            }
            sb.append("|\n")
        }

        sb.append(rowSeparator)
        return sb.toString()
    }
}