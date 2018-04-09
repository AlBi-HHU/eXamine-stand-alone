package org.hhu.examine.data.model

import org.hhu.examine.data.table.Column
import org.hhu.examine.data.table.Row
import org.hhu.examine.data.table.Table
import org.hhu.examine.data.table.emptyTable

const val IDENTIFIER_COLUMN_NAME = "Identifier"
const val CATEGORY_COLUMN_NAME = "Category"

abstract class NetworkRow(override val index: Int) : Row {

    override fun equals(other: Any?) = other != null &&
            other is NetworkRow &&
            this.javaClass == other.javaClass &&
            this.index == other.index

    override fun hashCode() = index

}

class NetworkNode(index: Int) : NetworkRow(index)

class NetworkLink(index: Int, val source: NetworkNode, val target: NetworkNode) : NetworkRow(index)

class NetworkAnnotation(index: Int, val nodes: Set<NetworkNode>) : NetworkRow(index)

class NetworkTable<R : NetworkRow>(
        override val rows: List<R>,
        val stringColumns: Table<R, String>,
        val numberColumns: Table<R, Double>,
        val hrefColumns: Table<R, String>
) : Table<R, Any> {

    override val columns = stringColumns.columns + numberColumns.columns + hrefColumns.columns

    val identities: Column<String> = stringColumns[IDENTIFIER_COLUMN_NAME]

    val categories: Column<String> = stringColumns[CATEGORY_COLUMN_NAME]

    override fun select(rows: List<R>): NetworkTable<R> = NetworkTable(
            rows,
            stringColumns.select(rows),
            numberColumns.select(rows),
            hrefColumns.select(rows)
    )

    override fun filter(predicate: (R) -> Boolean): NetworkTable<R> = select(rows.filter(predicate))

    fun map(transform: (R) -> R): NetworkTable<R> = select(rows.map(transform))

}

fun <R : NetworkRow> emptyNetworkTable() = NetworkTable<R>(
        emptyList(),
        emptyTable(),
        emptyTable(),
        emptyTable()
)