package org.hhu.examine.main.annotation

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import org.hhu.examine.data.model.NetworkAnnotation
import org.hhu.examine.main.MainViewModel
import org.hhu.examine.property.bind
import java.util.concurrent.Callable
import java.util.function.Consumer

class AnnotationTabs(private val model: MainViewModel) : TabPane() {

    init {
        styleClass.add("annotation-tabs")
        tabs.bind(model.dataSetProperty(), { it.categories.keys.map(::createTab) })
    }

    private fun createTab(category: String): AnnotationTab {
        val tab = AnnotationTab(model, category)
        tab.annotationColorsProperty().set(model.annotationColors)
        tab.highlightedAnnotationsProperty().set(model.highlightedAnnotations)
        tab.onToggleAnnotationProperty().set(Consumer { model.toggleAnnotation(it) })
        tab.onHighlightAnnotationsProperty().set(Consumer { model.hover(it) })
        return tab
    }

    override fun getUserAgentStylesheet(): String =
            AnnotationTabs::class.java.getResource("AnnotationTabs.css").toExternalForm()

}

internal class AnnotationTab(private val model: MainViewModel, category: String) : Tab() {

    private val annotationTable: TableView<NetworkAnnotation>
    private val annotationSelectionModel: AnnotationSelectionModel

    private val annotationColors = SimpleMapProperty(FXCollections.observableHashMap<NetworkAnnotation, Color>())

    private val colorColumn = TableColumn<NetworkAnnotation, Color?>()
    private val nameColumn = TableColumn<NetworkAnnotation, String>()
    private val scoreColumn = TableColumn<NetworkAnnotation, Double>("Score")
    private val urlColumn = TableColumn<NetworkAnnotation, String>()

    private val onToggleAnnotation = SimpleObjectProperty<Consumer<NetworkAnnotation>>(Consumer { _ -> })
    private val onHighlightAnnotations = SimpleObjectProperty<Consumer<NetworkAnnotation?>>(Consumer { _ -> })

    init {

        // Tab.
        isClosable = false
        text = category

        // Table.
        val annotations = model.dataSet.categories[category] ?: emptyList()
        annotationTable = TableView(FXCollections.observableList(annotations))

        val content = BorderPane(annotationTable)
        content.styleClass.add("annotation-tab")
        setContent(content)

        nameColumn.text = category

        // Cell value factories.
        colorColumn.setCellValueFactory { bindColorValue(it) }
        nameColumn.setCellValueFactory {
            SimpleStringProperty(
                    model.dataSet.annotations.stringColumns["Symbol"]?.get(it.value)
            )
        }
        scoreColumn.setCellValueFactory {
            SimpleObjectProperty(
                    model.dataSet.annotations.numberColumns["Score"]?.get(it.value)
            )
        }
        urlColumn.setCellValueFactory {
            SimpleStringProperty(
                    model.dataSet.annotations.hrefColumns["URL"]?.get(it.value)
            )
        }

        // Cell factories.

        // Row and cell factories.
        annotationTable.setRowFactory(::createRow)
        colorColumn.setCellFactory(::createColorCell)
        urlColumn.setCellFactory(::createUrlCell)

        // Column layout and style.
        colorColumn.styleClass.add("color-column")
        nameColumn.styleClass.add("name-column")
        scoreColumn.styleClass.add("score-column")
        urlColumn.styleClass.add("url-column")

        annotationTable.columns.setAll(colorColumn, nameColumn, scoreColumn, urlColumn)

        annotationSelectionModel = AnnotationSelectionModel(annotationTable)
        annotationTable.selectionModel = annotationSelectionModel
        annotationSelectionModel.onToggleAnnotationProperty().bind(onToggleAnnotation)

        annotationTable.setRowFactory(::createRow)
    }

    private fun bindColorValue(parameters: TableColumn.CellDataFeatures<NetworkAnnotation, Color?>): ObservableValue<Color?> {
        return Bindings.createObjectBinding(Callable { annotationColors[parameters.value] }, annotationColors)
    }

    private fun createRow(tableView: TableView<NetworkAnnotation>): TableRow<NetworkAnnotation> {

        val tableRow = TableRow<NetworkAnnotation>()
        tableRow.setOnMouseEntered { _ ->
            onHighlightAnnotations.get().accept(tableRow.item)
        }
        tableRow.setOnMouseExited { _ -> onHighlightAnnotations.get().accept(null) }

        return tableRow
    }

    private fun createColorCell(column: TableColumn<NetworkAnnotation, Color?>): TableCell<NetworkAnnotation, Color?> {
        return object : TableCell<NetworkAnnotation, Color?>() {

            init {
                styleClass.add("color-cell")
            }

            override fun updateItem(optionalColor: Color?, empty: Boolean) {
                graphic = if (empty || optionalColor == null) {
                    null
                } else {
                    annotationMarker(optionalColor)
                }
            }
        }
    }

    private fun createUrlCell(column: TableColumn<NetworkAnnotation, String?>): TableCell<NetworkAnnotation, String?> {
        return object : TableCell<NetworkAnnotation, String?>() {

            override fun updateItem(optionalUrl: String?, empty: Boolean) {
                graphic = if (empty || optionalUrl == null || optionalUrl == "about:blank") {
                    null
                } else {
                    val link = Label("\uD83C\uDF10")
                    link.setOnMouseClicked { model.openBrowser(optionalUrl) }
                    link
                }
            }
        }
    }

    fun annotationColorsProperty() = annotationColors

    fun highlightedAnnotationsProperty() = annotationSelectionModel.highlightedAnnotationsProperty()

    fun onToggleAnnotationProperty() = onToggleAnnotation

    fun onHighlightAnnotationsProperty() = onHighlightAnnotations

}

fun annotationMarker(color: Color): Node {
    val marker = Circle()

    marker.radius = 6.0
    marker.fill = color
    marker.style = "-fx-stroke: -base-intensity-low; -fx-stroke-width: 2px;"

    return marker
}