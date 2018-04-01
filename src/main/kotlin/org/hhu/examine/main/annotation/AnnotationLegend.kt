package org.hhu.examine.main.annotation

import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.Skin
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.hhu.examine.data.model.NetworkAnnotation
import org.hhu.examine.main.MainViewModel
import tornadofx.bind
import tornadofx.em
import tornadofx.style

class AnnotationLegend(val model: MainViewModel) : Control() {

    override fun createDefaultSkin(): Skin<AnnotationLegend> = AnnotationLegendSkin(this)

}

private class AnnotationLegendSkin(private val legend: AnnotationLegend) : Skin<AnnotationLegend> {

    private val root = VBox()

    init {
        root.children.bind(legend.model.selectedAnnotationsProperty(), ::createLabel)
        root.isFillWidth = true

        root.maxHeight = VBox.USE_PREF_SIZE
    }

    private fun createLabel(annotation: NetworkAnnotation): Label {
        val label = Label(legend.model.activeNetwork.dataSet.annotations.stringColumns["Symbol"]?.get(annotation))

        with(label) {
            graphic = annotationMarker(legend.model.annotationColors[annotation] ?: Color.TRANSPARENT)

            style {
                minHeight = 2.em
                maxHeight = 2.em
            }
        }

        return label
    }

    override fun getSkinnable(): AnnotationLegend = legend

    override fun getNode(): Node = root

    override fun dispose() {}

}