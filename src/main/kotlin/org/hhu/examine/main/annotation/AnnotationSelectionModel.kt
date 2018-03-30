package org.hhu.examine.main.annotation

import javafx.beans.property.SetProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleSetProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableSet
import javafx.collections.ObservableList
import javafx.collections.SetChangeListener
import javafx.scene.control.TableColumn
import javafx.scene.control.TablePosition
import javafx.scene.control.TableView
import org.hhu.examine.data.model.NetworkAnnotation
import java.util.function.Consumer
import java.util.stream.Collectors.toList

/**
 * Builds a default TableViewSelectionModel instance with the provided
 * TableView.
 *
 * @param tableView The TableView upon which this selection model should operate.
 * @throws NullPointerException TableView can not be null.
 */
internal class AnnotationSelectionModel(tableView: TableView<NetworkAnnotation>) : TableView.TableViewSelectionModel<NetworkAnnotation>(tableView) {

    private val highlightedAnnotations = SimpleSetProperty(observableSet<NetworkAnnotation>())
    private val onToggleAnnotationProperty = SimpleObjectProperty<Consumer<NetworkAnnotation>>(Consumer { _ -> })

    private val highlightedTablePositions = observableArrayList<TablePosition<*, *>>()

    init {
        highlightedAnnotations.addListener(SetChangeListener { _ -> updateHighlightedTablePositions() })
    }

    private fun updateHighlightedTablePositions() {

        val newPositions = highlightedAnnotations.stream()
                .filter({ tableModel.contains(it) })
                .map { annotation -> TablePosition<NetworkAnnotation, Any>(tableView, tableModel.indexOf(annotation), null) }
                .collect(toList())

        highlightedTablePositions.setAll(newPositions)
    }

    fun highlightedAnnotationsProperty(): SetProperty<NetworkAnnotation> {
        return highlightedAnnotations
    }

    fun onToggleAnnotationProperty(): SimpleObjectProperty<Consumer<NetworkAnnotation>> {
        return onToggleAnnotationProperty
    }

    override fun getSelectedCells(): ObservableList<TablePosition<*, *>> {
        return highlightedTablePositions
    }

    override fun isSelected(row: Int, column: TableColumn<NetworkAnnotation, *>): Boolean {
        return isSelected(row)
    }

    override fun isSelected(row: Int): Boolean {
        return row < tableModel.size && highlightedAnnotations.contains(tableModel[row])
    }

    override fun select(row: Int, column: TableColumn<NetworkAnnotation, *>) {}

    override fun clearAndSelect(row: Int, column: TableColumn<NetworkAnnotation, *>) {
        onToggleAnnotationProperty().get().accept(tableModel[row])
    }

    override fun clearSelection(row: Int, column: TableColumn<NetworkAnnotation, *>) {

    }

    override fun selectLeftCell() {

    }

    override fun selectRightCell() {

    }

    override fun selectAboveCell() {

    }

    override fun selectBelowCell() {

    }
}
