package org.cwi.examine.presentation.nodelinkcontour

import com.vividsolutions.jts.geom.Geometry
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.binding.Bindings.valueAt
import javafx.beans.property.ListProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableHashMap
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.cwi.examine.model.NetworkAnnotation
import org.cwi.examine.presentation.nodelinkcontour.layout.Contours
import java.util.concurrent.Callable

internal class ContourLayer : StackPane() {

    private val annotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val contours = SimpleMapProperty(observableHashMap<NetworkAnnotation, Contours>())
    private val colors = SimpleMapProperty(observableHashMap<NetworkAnnotation, Color>())

    private val ribbonLayer = NetworkElementLayer("network-contour", this::createAnnotationRibbon)

    init {
        children.setAll(ribbonLayer)
        ribbonLayer.elementProperty().bind(annotations)
    }

    private fun createAnnotationRibbon(annotation: NetworkAnnotation): Node {

        val ribbon = ContourRepresentation(annotation)
        ribbon.styleClass.add("network-contour-ribbon")
        ribbon.geometryProperty().bind(createObjectBinding(
                Callable { contours.getOrDefault(annotation, Contours(annotation)).ribbon },
                contours))
        ribbon.fillProperty().bind(valueAt(colors, annotation))

        val hardOutline = ContourRepresentation(annotation)
        hardOutline.styleClass.add("network-contour-hard-outline")
        hardOutline.geometryProperty().bind(createObjectBinding<Geometry>(
                Callable { contours.getOrDefault(annotation, Contours(annotation)).outline },
                contours
        ))

        return Pane(ribbon, hardOutline)
    }

    fun annotationsProperty(): ListProperty<NetworkAnnotation> {
        return annotations
    }

    fun contoursProperty(): MapProperty<NetworkAnnotation, Contours> {
        return contours
    }

    fun colorsProperty(): MapProperty<NetworkAnnotation, Color> {
        return colors
    }

}
