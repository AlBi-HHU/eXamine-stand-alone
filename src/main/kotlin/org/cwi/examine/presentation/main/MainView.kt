package org.cwi.examine.presentation.main

import javafx.geometry.Side
import javafx.scene.layout.BorderPane
import org.cwi.examine.presentation.main.annotation.AnnotationTabs
import org.cwi.examine.presentation.nodelinkcontour.NodeLinkContourView

/**
 * Primary pane of the application.
 */
class MainView : BorderPane() {

    val nodeLinkContourView = NodeLinkContourView()
    val annotationOverview = AnnotationTabs()

    init {

        styleClass.add("main-view")

        val nodeLinkContourContainer = BorderPane(nodeLinkContourView)
        nodeLinkContourContainer.styleClass.add("node-link-contour-container")
        center = nodeLinkContourContainer

        annotationOverview.side = Side.RIGHT
        left = annotationOverview
    }

    override fun getUserAgentStylesheet(): String {
        return MainView::class.java.getResource("MainView.css").toExternalForm()
    }
}
