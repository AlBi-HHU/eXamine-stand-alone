package org.hhu.examine.data.model

import org.hhu.examine.data.table.Column
import org.hhu.examine.data.table.Row
import org.hhu.examine.data.table.Table
import org.hhu.examine.data.table.emptyColumn

class DataTable<out R : Row>(
        override val rows: List<R>,
        val stringColumns: Map<String, Column<String>>,
        val numberColumns: Map<String, Column<Double>>,
        val hrefColumns: Map<String, Column<String>>
) : Table<R, Any> {

    override val columns = stringColumns + numberColumns + hrefColumns

    val identities: Column<String> = stringColumns["Identity"] ?: emptyColumn("Identity")

    val categories: Column<String> = stringColumns["Category"] ?: emptyColumn("Category")

}

fun <R : Row> emptyDataTable() = DataTable<R>(
        emptyList(),
        emptyMap(),
        emptyMap(),
        emptyMap()
)