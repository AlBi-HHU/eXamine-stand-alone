package org.hhu.examine.data.table

import org.hhu.examine.math.Interval
import org.hhu.examine.math.extrema as doubleExtrema

fun Column<Double>.extrema(rows: Collection<Row>): Interval? = doubleExtrema(sliceNotNull(rows.toList()))