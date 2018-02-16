package org.cwi.examine.presentation.main.annotation

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.FXCollections.observableList
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.cwi.examine.model.NetworkAnnotation
import org.cwi.examine.model.NetworkCategory
import java.util.Arrays.asList
import java.util.Collections.emptyList
import java.util.concurrent.Callable
import java.util.function.Consumer

internal class AnnotationTab(category: NetworkCategory) : Tab() {

    private val annotationTable: TableView<NetworkAnnotation>
    private val annotationSelectionModel: AnnotationSelectionModel

    private val annotationColors = SimpleMapProperty(observableHashMap<NetworkAnnotation, Color>())

    private val colorColumn = TableColumn<NetworkAnnotation, Color?>()
    private val nameColumn = TableColumn<NetworkAnnotation, String>()
    private val scoreColumn = TableColumn<NetworkAnnotation, Double>("Score")

    private val onToggleAnnotation = SimpleObjectProperty<Consumer<NetworkAnnotation>>(Consumer { _ -> })
    private val onHighlightAnnotations = SimpleObjectProperty<Consumer<List<NetworkAnnotation>>>(Consumer { _ -> })

    init {

        // Tab.
        isClosable = false
        text = category.name

        // Table.
        annotationTable = TableView(observableList(category.annotations))

        val content = BorderPane(annotationTable)
        content.styleClass.add("annotation-tab")
        setContent(content)

        nameColumn.text = category.name

        // Cell value factories.
        colorColumn.setCellValueFactory({ this.bindColorValue(it) })
        nameColumn.setCellValueFactory { parameters -> SimpleStringProperty(parameters.value.name) }
        scoreColumn.setCellValueFactory { parameters -> SimpleObjectProperty(parameters.value.score) }

        // Row and cell factories.
        annotationTable.setRowFactory({ this.createRow(it) })
        colorColumn.setCellFactory({ this.createColorCell(it) })

        // Column layout and style.
        colorColumn.styleClass.add("color-column")
        nameColumn.styleClass.add("name-column")
        scoreColumn.styleClass.add("score-column")

        annotationTable.columns.setAll(colorColumn, nameColumn, scoreColumn)

        annotationSelectionModel = AnnotationSelectionModel(annotationTable)
        annotationTable.selectionModel = annotationSelectionModel
        annotationSelectionModel.onToggleAnnotationProperty().bind(onToggleAnnotation)

        annotationTable.setRowFactory({ this.createRow(it) })
    }

    private fun bindColorValue(parameters: TableColumn.CellDataFeatures<NetworkAnnotation, Color?>): ObservableValue<Color?> {
        return Bindings.createObjectBinding(Callable { annotationColors[parameters.value] }, annotationColors)
    }

    private fun createRow(tableView: TableView<NetworkAnnotation>): TableRow<NetworkAnnotation> {

        val tableRow = TableRow<NetworkAnnotation>()
        tableRow.setOnMouseEntered { _ -> onHighlightAnnotations.get().accept(asList(tableRow.item)) }
        tableRow.setOnMouseExited { _ -> onHighlightAnnotations.get().accept(emptyList()) }

        return tableRow
    }

    private fun createColorCell(column: TableColumn<NetworkAnnotation, Color?>): TableCell<NetworkAnnotation, Color?> {
        return object : TableCell<NetworkAnnotation, Color?>() {

            init {
                styleClass.add("color-cell")
            }

            override fun updateItem(optionalColor: Color?, empty: Boolean) {

                val marker: Pane?

                if (empty || optionalColor == null) {
                    marker = null
                } else {
                    marker = Pane()
                    marker.styleClass.add("marker")
                    marker.style = "-fx-background-color: " + rgbString(optionalColor)
                }

                graphic = marker
            }
        }
    }

    private fun rgbString(color: Color): String {
        return "rgba(" +
                (255 * color.red).toInt() + "," +
                (255 * color.green).toInt() + "," +
                (255 * color.blue).toInt() + "," +
                color.opacity + ")"
    }

    fun annotationColorsProperty(): MapProperty<NetworkAnnotation, Color> {
        return annotationColors
    }

    fun highlightedAnnotationsProperty(): SetProperty<NetworkAnnotation> {
        return annotationSelectionModel.highlightedAnnotationsProperty()
    }

    fun onToggleAnnotationProperty(): SimpleObjectProperty<Consumer<NetworkAnnotation>> {
        return onToggleAnnotation
    }

    fun onHighlightAnnotationsProperty(): SimpleObjectProperty<Consumer<List<NetworkAnnotation>>> {
        return onHighlightAnnotations
    }

}
