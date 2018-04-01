package org.hhu.examine.data.model

import org.hhu.examine.data.table.Column
import org.hhu.examine.data.table.Row
import org.hhu.examine.data.table.Table
import org.hhu.examine.data.table.emptyTable

class DataTable<out R : Row>(
        override val rows: List<R>,
        val stringColumns: Table<R, String>,
        val numberColumns: Table<R, Double>,
        val hrefColumns: Table<R, String>
) : Table<R, Any> {

    override val columns = stringColumns.columns + numberColumns.columns + hrefColumns.columns

    val identities: Column<String> = stringColumns["Identifier"]

    val categories: Column<String> = stringColumns["Category"]

}

fun <R : Row> emptyDataTable() = DataTable<R>(
        emptyList(),
        emptyTable(),
        emptyTable(),
        emptyTable()
)