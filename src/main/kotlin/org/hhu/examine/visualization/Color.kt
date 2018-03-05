package org.hhu.examine.visualization.color

import javafx.scene.paint.Color
import org.hhu.examine.math.Interval
import kotlin.math.round

interface Colormap<in E> : (E) -> Color

internal val INVALID_COLOR = Color.MAGENTA

internal class ColormapByCSV(private val fileName: String) : Colormap<Double> {

    private val colors: Array<Color> = loadColors()
    private val binSize = (colors.size - 1).toDouble()

    override fun invoke(value: Double) = colorBoundaryConditions(value)

    private fun lookup(value: Double): Color = colors[round(value * binSize).toInt()]

    private fun colorBoundaryConditions(value: Double): Color =
            if (value.isNaN() || value < 0.0 || value > 1.0) INVALID_COLOR else lookup(value)

    private fun loadColors(): Array<Color> =
            javaClass.getResource(fileName)
                    .readText()
                    .split("\n")
                    .map { it.split(",") }
                    .map { Color.color(it[0].toDouble(), it[1].toDouble(), it[2].toDouble()) }
                    .toTypedArray()

}

val BlueWhiteRed: Colormap<Double> = ColormapByCSV("color_cet_diverging_bwr_40-95_c42_n256.csv")

data class ColormapInterval(val colormap: Colormap<Double>, val interval: Interval) : Colormap<Double> {

    override fun invoke(value: Double) = colormap(interval.absoluteToFactor(value))

}