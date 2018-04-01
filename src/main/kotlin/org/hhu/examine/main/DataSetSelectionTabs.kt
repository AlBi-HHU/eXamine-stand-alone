package org.hhu.examine.main

import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import org.hhu.examine.main.nodelinkcontour.NodeLinkContourView
import tornadofx.bind
import java.io.File
import java.util.concurrent.Callable

/**
 * Tabs that show each data set as a tab to the far left of the screen.
 * Clicking on a tab results in switching to the tab's data set.
 */
class DataSetSelectionTabs(private val model: MainViewModel) : StackPane() {

    private val contentPlaceholder = Label(
            "Please place your data sets in the '" +
                    MainViewModel.DATA_SET_DIRECTORY +
                    "' directory and try again.")
    private val tabPane = TabPane()
    private val loadingLabel = StackPane(Label("Loading network..."))

    init {
        alignment = Pos.TOP_CENTER

        contentPlaceholder.visibleProperty().bind(Bindings.isEmpty(model.dataSets))

        tabPane.styleClass += "network-tabs"
        tabPane.side = Side.LEFT
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        tabPane.tabs.bind(model.dataSets, ::createPane)

        tabPane.selectionModel.selectedIndexProperty().addListener { _, _, newIndex ->
            model.activateDataSet(model.dataSets[newIndex.toInt()])
        }

        children.addAll(contentPlaceholder, tabPane)
    }

    private fun createPane(dataSet: File): Tab {
        val pane = Tab(dataSet.name, loadingLabel)

        pane.contentProperty().bind(Bindings.createObjectBinding(
                Callable {
                    if (model.activeNetwork.dataSet.name == dataSet.name)
                        NodeLinkContourView(model)
                    else
                        loadingLabel
                }, model.activeNetworkProperty()))

        return pane
    }

    fun getImageExportableRegion(): Region? =
            if (tabPane.selectionModel.isEmpty)
                null
            else {
                val tabPaneContent = tabPane.selectionModel.selectedItem.content
                (tabPaneContent as? NodeLinkContourView)?.getImageExportableRegion()
            }

}