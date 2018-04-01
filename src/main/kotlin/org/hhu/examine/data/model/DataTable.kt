package org.hhu.examine.data.model

import org.hhu.examine.data.table.Column
import org.hhu.examine.data.table.Row
import org.hhu.examine.data.table.Table
import org.hhu.examine.data.table.emptyTable

const val IDENTIFIER_COLUMN_NAME = "Identifier"
const val CATEGORY_COLUMN_NAME = "Category"

class DataTable<R : Row>(
        override val rows: List<R>,
        val stringColumns: Table<R, String>,
        val numberColumns: Table<R, Double>,
        val hrefColumns: Table<R, String>
) : Table<R, Any> {

    override val columns = stringColumns.columns + numberColumns.columns + hrefColumns.columns

    val identities: Column<String> = stringColumns[IDENTIFIER_COLUMN_NAME]

    val categories: Column<String> = stringColumns[CATEGORY_COLUMN_NAME]

    override fun select(rows: List<R>): DataTable<R> = DataTable(
            rows,
            stringColumns.select(rows),
            numberColumns.select(rows),
            hrefColumns.select(rows)
    )

    override fun filter(predicate: (R) -> Boolean): DataTable<R> = select(rows.filter(predicate))

}

fun <R : Row> emptyDataTable() = DataTable<R>(
        emptyList(),
        emptyTable(),
        emptyTable(),
        emptyTable()
)