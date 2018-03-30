package org.hhu.examine.data.table

class DenseColumn<out V>(
        override val identifier: String,
        private val values: Array<V?>
) : Column<V> {

    override fun get(entry: Row) = values[entry.index]

}

class DenseDoubleColumn(
        override val identifier: String,
        private val values: DoubleArray
) : Column<Double> {

    override fun get(entry: Row): Double? {
        val value = values[entry.index]
        return if (value == Double.NaN) null else value
    }

}