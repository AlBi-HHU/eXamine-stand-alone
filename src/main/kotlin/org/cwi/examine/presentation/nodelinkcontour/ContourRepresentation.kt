package org.cwi.examine.presentation.nodelinkcontour

import com.vividsolutions.jts.geom.Geometry
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.shape.Path
import org.cwi.examine.model.NetworkAnnotation
import org.cwi.examine.presentation.nodelinkcontour.layout.Paths
import java.util.Collections.emptyList

internal class ContourRepresentation(private val annotation: NetworkAnnotation) : Path() {

    private val geometry = SimpleObjectProperty(Paths.GEOMETRY_FACTORY.buildGeometry(emptyList<Any>()))

    init {
        geometry.addListener(::geometryChange)
    }

    private fun geometryChange(observable: ObservableValue<out Geometry>, oldGeometry: Geometry, geometry: Geometry) {
        elements.setAll(Paths.geometryToShape(geometry))
    }

    fun geometryProperty(): ObjectProperty<Geometry> {
        return geometry
    }
}
