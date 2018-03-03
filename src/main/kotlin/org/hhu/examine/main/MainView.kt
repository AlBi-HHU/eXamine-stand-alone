package org.hhu.examine.main

import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import org.hhu.examine.main.annotation.AnnotationTabs
import org.hhu.examine.visualization.control.ColorBar
import org.hhu.examine.visualization.imageToClipboard
import org.hhu.examine.visualization.imageToFileByDialog
import org.hhu.examine.visualization.regionToImage
import tornadofx.*
import java.util.Arrays.asList

/** Primary pane of the application. */
class MainView : View() {

    private val model: MainViewModel by inject()

    override val root = BorderPane()

    private val colorLegend = ColorLegend(model)
    private val selectionTabs = DataSetSelectionTabs(model)

    init {
        root.stylesheets += javaClass.getResource("MainView.css").toExternalForm()
        root.styleClass += "main-view"

        titleProperty.bind(model.activeDataSetProperty().stringBinding { dataSet ->
            "eXamine" + (dataSet?.name?.let { name -> " - $name" } ?: "")
        })

        // Annotation lists at the left.
        val annotationOverview = AnnotationTabs(model)
        annotationOverview.side = Side.LEFT
        root.right = annotationOverview

        // Node link contour and controls at the center.
        val nodeLinkContainer = BorderPane()
        nodeLinkContainer.style = "-fx-padding: 0 .5em .5em 0"
        root.center = nodeLinkContainer

        nodeLinkContainer.center = selectionTabs

        // Information bar at the top.
        nodeLinkContainer.top = hbox {
            children += colorLegend
            children += ExportActions(::exportToClipBoard, ::exportToFile)

            style {
                spacing = 4.em
                padding = box(0.em, 4.em, 0.em, 6.em)
                minHeight = 2.5.em
                maxHeight = 2.5.em
            }
        }
    }

    private fun exportToClipBoard() {
        exportImage()?.let(::imageToClipboard)
    }

    private fun exportToFile() {
        exportImage()?.let(::imageToFileByDialog)
    }

    private fun exportImage(): Image? = selectionTabs.getImageExportableRegion()?.let { region ->
        regionToImage(region, 1.em.value, colorLegend)
    }

}

/** Color bar with a score label. */
private class ColorLegend(model: MainViewModel) : HBox() {

    init {
        alignment = Pos.CENTER

        val barLabel = label("Score") {
            style {
                padding = box(0.em, 0.5.em, 0.em, 0.em)
            }
        }
        barLabel.visibleProperty().bind(model.nodeColormap().isNotNull)

        val colorBar = ColorBar()
        colorBar.colormapProperty().bind(model.nodeColormap())
        children += colorBar
    }

}

private class ExportActions(
        exportToClipboard: () -> Unit,
        exportToFile: () -> Unit) : HBox() {

    init {
        alignment = Pos.CENTER

        val exportLabel = Label("Export image to")
        val clipBoardLink = Hyperlink("clipboard")
        val orLabel = Label("or")
        val fileLink = Hyperlink("file")

        clipBoardLink.action(exportToClipboard)
        fileLink.action(exportToFile)

        children += asList(exportLabel, clipBoardLink, orLabel, fileLink)
    }

}