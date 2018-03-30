package org.hhu.examine.data.csv.columnwriter

import org.hhu.examine.data.table.Column
import org.hhu.examine.data.table.DenseColumn
import org.hhu.examine.data.table.DenseDoubleColumn

const val COLUMN_TYPE_DELIMITER = ':'
const val COLUMN_TYPE_TAG_STRING = "String"
const val COLUMN_TYPE_TAG_NUMBER = "Number"
const val COLUMN_TYPE_TAG_HREF = "Href"

interface ColumnWriter<out C> {
    val identifier: String
    val column: Column<C>
    fun writeValue(index: Int, value: String)
}

class ColumnWriters(rowCount: Int, columnHeaders: List<String>) {
    private val completeHeaders = addImplicitColumnTypes(columnHeaders)

    val stringWriters: List<ColumnWriter<String>> =
            (columnIdentifiersForType(completeHeaders, COLUMN_TYPE_TAG_STRING) + columnIdentifiersForNoType(completeHeaders))
                    .map { StringColumnWriter(it, rowCount) }
    val numberWriters: List<ColumnWriter<Double>> = columnIdentifiersForType(completeHeaders, COLUMN_TYPE_TAG_NUMBER)
            .map { NumberColumnWriter(it, rowCount) }
    val hrefWriters: List<ColumnWriter<String>> = columnIdentifiersForType(completeHeaders, COLUMN_TYPE_TAG_HREF)
            .map { StringColumnWriter(it, rowCount) }

    val writerMap = (stringWriters + numberWriters + hrefWriters)
            .map { Pair(it.identifier, it) }
            .toMap()
}

/** Add implicit column type for certain column names. */
private fun addImplicitColumnTypes(columnHeaders: List<String>) = columnHeaders.map {
    it + when (it.trim()) {
        "Score" -> COLUMN_TYPE_DELIMITER + COLUMN_TYPE_TAG_NUMBER
        "URL" -> COLUMN_TYPE_DELIMITER + COLUMN_TYPE_TAG_HREF
        else -> ""
    }
}

/** Column identifiers of those columns that have the matching type tag. */
private fun columnIdentifiersForType(columnHeaders: List<String>, typeTag: String): List<String> = columnHeaders
        .filter { header -> header.count { it == COLUMN_TYPE_DELIMITER } == 1 }
        .map { it.split(COLUMN_TYPE_DELIMITER).map(String::trim) }
        .filter { it.last() == typeTag }
        .map { it.first() }

/** Column identifiers of those columns that have no type tag. */
private fun columnIdentifiersForNoType(columnHeaders: List<String>): List<String> = columnHeaders
        .filter { header -> header.count { it == ':' } == 0 }
        .map(String::trim)

private class StringColumnWriter(override val identifier: String, size: Int) : ColumnWriter<String> {
    private val values = Array<String?>(size, { _ -> null })
    override val column = DenseColumn(identifier, values)

    override fun writeValue(index: Int, value: String) {
        values[index] = value
    }
}

private class NumberColumnWriter(override val identifier: String, size: Int) : ColumnWriter<Double> {
    private val values = DoubleArray(size, { _ -> Double.NaN })
    override val column = DenseDoubleColumn(identifier, values)

    override fun writeValue(index: Int, value: String) {
        values[index] = if (value.isBlank()) Double.NaN else value.toDouble()
    }
}