package org.cwi.examine.main

import javafx.geometry.Side
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import org.cwi.examine.main.annotation.AnnotationTabs
import org.cwi.examine.main.nodelinkcontour.NodeLinkContourView
import org.cwi.examine.visualization.control.ColorBar
import tornadofx.*
import java.util.function.Consumer

/** Primary pane of the application. */
class MainView : View("eXamine") {

    private val model: MainViewModel by inject()

    override val root = BorderPane()

    init {
        root.styleClass.add("main-view")

        // Annotation lists at the left.
        val annotationOverview = AnnotationTabs()
        annotationOverview.side = Side.RIGHT
        root.left = annotationOverview

        annotationOverview.categoriesProperty().bindContent(model.categories)
        annotationOverview.annotationColorsProperty().bind(model.annotationColorProperty())
        annotationOverview.highlightedAnnotationsProperty().bind(model.highlightedAnnotations())
        annotationOverview.onToggleAnnotationProperty().set(Consumer { model.toggleAnnotation(it) })
        annotationOverview.onHighlightAnnotationsProperty().set(Consumer { model.highlightAnnotations(it) })

        // Node link contour and controls at the center.
        val nodeLinkContainer = BorderPane()
        nodeLinkContainer.style = "-fx-padding: 0 .5em .5em .5em"
        root.center = nodeLinkContainer

        nodeLinkContainer.center = NodeLinkContourView(model)

        // Information bar at the top.
        val colorBar = ColorBar()
        colorBar.colormapProperty().bind(model.nodeColormap())
        val barLabel = label("Score") {
            style {
                padding = box(0.em, 0.5.em, 0.em, 0.em)
            }
        }

        val informationBar = BorderPane()
        informationBar.left = HBox(barLabel, colorBar)

        nodeLinkContainer.top = informationBar
    }

}
