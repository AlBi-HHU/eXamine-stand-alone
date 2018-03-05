package org.hhu.examine.visualization.control

import javafx.beans.binding.Bindings.createStringBinding
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.shape.StrokeLineCap
import org.hhu.examine.visualization.color.ColormapInterval
import tornadofx.*
import java.util.concurrent.Callable
import kotlin.math.round

class ColorBar : Control() {

    var colormap: ColormapInterval? by property<ColormapInterval>()
    fun colormapProperty() = getProperty(ColorBar::colormap)

    override fun createDefaultSkin(): Skin<ColorBar> = ColorBarSkin(this)

}

internal class ColorBarSkin(private val colorBar: ColorBar) : Skin<ColorBar> {

    private val colorSequence = ColorBarCanvas(colorBar)
    private val root = BorderPane()

    init {
        with(root) {
            center = colorSequence
            left = label {
                textProperty().bind(createStringBinding(
                        Callable { colorBar.colormap?.interval?.start?.let { format(it) } },
                        colorBar.colormapProperty()
                ))

                style {
                    padding = box(0.em, 0.5.em, 0.em, 0.em)
                }
            }
            right = label {
                textProperty().bind(createStringBinding(
                        Callable { colorBar.colormap?.interval?.end?.let { format(it) } },
                        colorBar.colormapProperty()
                ))

                style {
                    padding = box(0.em, 0.em, 0.em, 0.5.em)
                }
            }

            BorderPane.setAlignment(left, Pos.CENTER)
            BorderPane.setAlignment(right, Pos.CENTER)
        }

        with(colorSequence) {
            style {
                minWidth = 5.em
                maxWidth = 5.em
                minHeight = 1.em
                maxHeight = 1.em
            }
        }

        root.visibleProperty().bind(colorBar.colormapProperty().isNotNull)
    }

    private fun format(value: Double) = "%.2f".format(value)

    override fun getSkinnable() = colorBar

    override fun getNode() = root

    override fun dispose() {}

}

internal class ColorBarCanvas(private val colorBar: ColorBar) : Pane() {

    private val canvas = Canvas()

    init {
        children.add(canvas)
        colorBar.colormapProperty().addListener({ _, _, _ -> draw() })
    }

    override fun layoutChildren() {
        canvas.width = width
        canvas.height = height
        draw()
    }

    private fun draw() {
        var g = canvas.graphicsContext2D
        g.clearRect(0.0, 0.0, width, height)
        g.lineCap = StrokeLineCap.BUTT

        val colormap = colorBar.colormap?.colormap
        if (colormap != null) {
            val snapWidth = round(width).toInt() - 1
            (0 until snapWidth).forEach {
                val value = (it.toDouble() + 0.5) / snapWidth.toDouble()
                g.stroke = colormap(value)
                g.strokeLine(it.toDouble() + .5, 0.0, it.toDouble() + .5, height)
            }
        }
    }

}