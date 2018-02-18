package org.cwi.examine.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Interval(val start: Double, val end: Double) {

    fun absoluteToFactor(absoluteValue: Double): Double = (absoluteValue - start) / (end - start)

    fun factorToAbsolute(factor: Double): Double = start + factor * (end - start)

    fun isValid(): Boolean = start.isFinite() && end.isFinite() && start != end

    fun min(): Double = min(start, end)

    fun max(): Double = max(start, end)

    fun contains(value: Double): Boolean = min() <= value && value <= max()

    fun expandToCenter(centerValue: Double): Interval? =
        if (contains(centerValue)) {
            val maxDistance = max(abs(centerValue - start), abs(centerValue - end))
            Interval(centerValue - maxDistance, centerValue + maxDistance)
        } else null

}

fun extrema(values: Collection<Double>): Interval? {
    val min = values.filter(Double::isFinite).min()
    val max = values.filter(Double::isFinite).max()

    return if (min != null && max != null && min != max)
        Interval(min, max)
    else
        null
}