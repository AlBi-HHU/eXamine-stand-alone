package org.hhu.examine.data.table

fun <C> emptyColumn(columnName: String): Column<C> = EmptyColumn(columnName)

operator fun <R : Row, C : Any> Table<R, C>.get(columnName: String): Column<C>? =
        columns[columnName]

fun <R : Row, C : Any> Table<R, C>.getNotNull(columnName: String): Column<C> =
        columns[columnName] ?: emptyColumn(columnName)

fun <C> Column<C>.slice(rows: List<Row>): List<C?> = rows.map(this::get)

fun <C : Any> Column<C>.sliceNotNull(rows: List<Row>): List<C> = rows.mapNotNull(this::get)


private class EmptyColumn<out V>(
        override val identifier: String
) : Column<V> {

    override fun get(entry: Row): V? = null

}