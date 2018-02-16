package org.cwi.examine.presentation.nodelinkcontour

import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ListChangeListener
import javafx.geometry.Point2D
import javafx.scene.shape.Path
import org.cwi.examine.presentation.nodelinkcontour.layout.Paths
import org.jgrapht.graph.DefaultEdge
import java.util.Collections.emptyList

internal class LinkRepresentation(private val edge: DefaultEdge) : Path() {

    private val controlPoints = SimpleListProperty(observableArrayList<Point2D>())

    init {
        controlPoints.addListener(ListChangeListener { _ ->
            elements.setAll(
                    if (controlPoints.size == 3)
                        Paths.getArc(controlPoints[0], controlPoints[1], controlPoints[2])
                    else
                        emptyList()
            )
        })
    }

    fun controlPointsProperty(): ListProperty<Point2D> {
        return controlPoints
    }
}
