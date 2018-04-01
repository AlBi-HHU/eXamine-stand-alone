package org.hhu.examine.data.table

interface Row {

    val index: Int

}

interface Column<out V : Any> {

    val identifier: String

    operator fun get(entry: Row): V?

    fun slice(rows: List<Row>): List<V?> = rows.map(this::get)

    fun sliceNotNull(rows: List<Row>): List<V> = rows.mapNotNull(this::get)

}

interface Table<out R : Row, out C : Any> {

    val rows: List<R>

    val columns: Map<String, Column<C>>

    operator fun get(columnName: String): Column<C> = columns[columnName] ?: emptyColumn(columnName)

}


fun <C : Any> emptyColumn(columnName: String): Column<C> = EmptyColumn(columnName)

fun <R : Row, C : Any> emptyTable(): Table<R, C> = SimpleTable(emptyList(), emptyMap())

private class EmptyColumn<out V : Any>(override val identifier: String) : Column<V> {

    override fun get(entry: Row): V? = null

}

class SimpleTable<out R : Row, out C : Any>(
        override val rows: List<R>,
        override val columns: Map<String, Column<C>>
) : Table<R, C>