package org.cwi.examine.main.nodelinkcontour

import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ListChangeListener
import javafx.geometry.Point2D
import javafx.scene.shape.Path
import org.cwi.examine.main.nodelinkcontour.layout.Paths

internal class LinkRepresentation : Path() {

    private val controlPoints = SimpleListProperty(observableArrayList<Point2D>())

    init {
        controlPoints.addListener(ListChangeListener { _ ->
            elements.clear()

            if (controlPoints.size == 3) {
                elements.addAll(Paths.getArc(controlPoints[0], controlPoints[1], controlPoints[2]))
            }
        })
    }

    fun controlPointsProperty(): ListProperty<Point2D> {
        return controlPoints
    }

}
