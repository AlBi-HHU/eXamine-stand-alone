package org.hhu.examine.main.annotation

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.hhu.examine.data.NetworkAnnotation
import org.hhu.examine.data.NetworkAnnotationCategory
import org.hhu.examine.main.MainViewModel
import tornadofx.bind
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer

class AnnotationTabs(private val model: MainViewModel) : TabPane() {

    init {
        styleClass.add("annotation-tabs")
        tabs.bind(model.activeCategories, ::createTab)
    }

    private fun createTab(category: NetworkAnnotationCategory): AnnotationTab {

        val tab = AnnotationTab(category)
        tab.annotationColorsProperty().set(model.annotationColors)
        tab.highlightedAnnotationsProperty().bind(model.highlightedAnnotations())
        tab.onToggleAnnotationProperty().set(Consumer { model.toggleAnnotation(it) })
        tab.onHighlightAnnotationsProperty().set(Consumer { model.highlightAnnotations(it) })

        return tab
    }

    override fun getUserAgentStylesheet(): String =
            AnnotationTabs::class.java.getResource("AnnotationTabs.css").toExternalForm()

}

internal class AnnotationTab(category: NetworkAnnotationCategory) : Tab() {

    private val annotationTable: TableView<NetworkAnnotation>
    private val annotationSelectionModel: AnnotationSelectionModel

    private val annotationColors = SimpleMapProperty(FXCollections.observableHashMap<NetworkAnnotation, Color>())

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
        annotationTable = TableView(FXCollections.observableList(category.annotations))

        val content = BorderPane(annotationTable)
        content.styleClass.add("annotation-tab")
        setContent(content)

        nameColumn.text = category.name

        // Cell value factories.
        colorColumn.setCellValueFactory { bindColorValue(it) }
        nameColumn.setCellValueFactory { SimpleStringProperty(it.value.name) }
        scoreColumn.setCellValueFactory { SimpleObjectProperty(it.value.score) }

        // Cell factories.

        // Row and cell factories.
        annotationTable.setRowFactory(::createRow)
        colorColumn.setCellFactory(::createColorCell)

        // Column layout and style.
        colorColumn.styleClass.add("color-column")
        nameColumn.styleClass.add("name-column")
        scoreColumn.styleClass.add("score-column")

        annotationTable.columns.setAll(colorColumn, nameColumn, scoreColumn)

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
        tableRow.setOnMouseEntered { _ -> onHighlightAnnotations.get().accept(
                if (tableRow.item == null) Collections.emptyList() else Arrays.asList(tableRow.item)) }
        tableRow.setOnMouseExited { _ -> onHighlightAnnotations.get().accept(Collections.emptyList()) }

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
