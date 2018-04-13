package org.hhu.examine.data.table

import org.hhu.examine.math.Interval
import java.util.*
import org.hhu.examine.math.extrema as doubleExtrema

fun Column<Double>.extrema(rows: Collection<Row>): Interval? = doubleExtrema(getNotNull(rows.toList()))

class DenseColumn<out V : Any>(
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