package org.hhu.examine.data.table

interface Row {
    val index: Int
}

interface Column<out V> {
    val identifier: String
    operator fun get(entry: Row): V?
}

interface Table<out R : Row, out C : Any> {
    val rows: List<R>
    val columns: Map<String, Column<C>>
}