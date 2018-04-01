package org.hhu.examine.data.table

import java.util.*

class DenseColumn<out V>(
        override val identifier: String,
        private val values: Array<V?>
) : Column<V> {

    override fun get(entry: Row): V? = values[entry.index]

    override fun toString(): String = "|" + values.size + "| " + Arrays.toString(values)

}

class DenseDoubleColumn(
        override val identifier: String,
        private val values: DoubleArray
) : Column<Double> {

    override fun get(entry: Row): Double? {
        val value = values[entry.index]
        return if (value == Double.NaN) null else value
    }

    override fun toString(): String = "|" + values.size + "| " + Arrays.toString(values)

}