package org.hhu.examine.main

import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.binding.Bindings.isEmpty
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import org.hhu.examine.data.DataSet
import org.hhu.examine.main.annotation.AnnotationTabs
import org.hhu.examine.main.nodelinkcontour.NodeLinkContourView
import org.hhu.examine.visualization.control.ColorBar
import tornadofx.*
import java.util.concurrent.Callable

/** Primary pane of the application. */
class MainView : View() {

    private val model: MainViewModel by inject()

    override val root = BorderPane()

    init {
        root.stylesheets += javaClass.getResource("MainView.css").toExternalForm()
        root.styleClass += "main-view"

        titleProperty.bind(model.activeDataSetProperty().stringBinding {
            "eXamine" + (it?.name?.let { name -> " - " + name } ?: "")
        })

        // Annotation lists at the left.
        val annotationOverview = AnnotationTabs(model)
        annotationOverview.side = Side.LEFT
        root.right = annotationOverview

        // Node link contour and controls at the center.
        val nodeLinkContainer = BorderPane()
        nodeLinkContainer.style = "-fx-padding: 0 .5em .5em .5em"
        root.center = nodeLinkContainer

        val selectionTabs = NetworkSelectionTabs(model)
        nodeLinkContainer.center = selectionTabs

        // Information bar at the top.
        val colorBar = ColorBar()
        colorBar.colormapProperty().bind(model.nodeColormap())
        val barLabel = label("Score") {
            style {
                padding = box(0.em, 0.5.em, 0.em, 0.em)
            }
        }
        barLabel.visibleProperty().bind(model.nodeColormap().isNotNull)

        val informationBar = BorderPane()
        informationBar.styleClass.add("information-bar")
        val informationBox = HBox(barLabel, colorBar)
        informationBox.alignment = Pos.CENTER
        informationBar.center = informationBox

        nodeLinkContainer.top = informationBar
    }

}

private class NetworkSelectionTabs(private val model: MainViewModel) : StackPane() {

    private val contentPlaceholder = Label(
            "Please place your data sets in the '" +
                    MainViewModel.DATA_SET_DIRECTORY +
                    "' directory and try again.")
    private val tabPane = TabPane()
    private val loadingLabel = StackPane(Label("Loading network..."))

    init {
        alignment = Pos.TOP_CENTER

        contentPlaceholder.visibleProperty().bind(isEmpty(model.dataSets))

        tabPane.styleClass += "network-tabs"
        tabPane.side = Side.LEFT
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tabPane.tabs.bind(model.dataSets, ::createPane)

        tabPane.selectionModel.selectedIndexProperty().addListener { _, _, newIndex ->
            model.activateDataSet(model.dataSets[newIndex.toInt()])
        }

        children.addAll(contentPlaceholder, tabPane)
    }

    private fun createPane(dataSet: DataSet): Tab {
        val pane = Tab(dataSet.name, loadingLabel)

        pane.contentProperty().bind(createObjectBinding(
                Callable {
                    if (model.activeDataSet == dataSet)
                        NodeLinkContourView(model)
                    else
                        loadingLabel
                }, model.activeNetworkProperty()))

        return pane
    }

}