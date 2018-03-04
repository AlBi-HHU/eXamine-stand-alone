package org.hhu.examine.main.nodelinkcontour

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleSetProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableSet
import javafx.collections.ListChangeListener
import javafx.collections.SetChangeListener
import javafx.scene.Node
import javafx.scene.layout.Pane
import java.util.*

internal class NetworkElementLayer<E, R : Node>(
        private val representationStyleClass: String,
        private val representationFactory: (E) -> R) : Pane() {

    private val elements = SimpleListProperty(observableArrayList<E>())
    private val highlightedElements = SimpleSetProperty(observableSet<E>())

    private val representations = HashMap<E, R>()

    init {
        elements.addListener(ListChangeListener { this.onElementChange(it) })
        highlightedElements.addListener(SetChangeListener { this.onHighlightedElementsChange(it) })
    }

    private fun onElementChange(change: ListChangeListener.Change<out E>) {
        change.next()

        representations.keys.removeAll(change.removed)
        change.addedSubList.forEach { element -> representations[element] = createRepresentation(element) }
        children.setAll(elements.map { representations[it] })
    }

    private fun onHighlightedElementsChange(change: SetChangeListener.Change<out E>) {
        if (change.wasAdded()) {
            representations[change.elementAdded]!!.styleClass.add(ELEMENT_HIGHLIGHTED_STYLE)
        } else if (change.wasRemoved()) {
            representations[change.elementRemoved]!!.styleClass.remove(ELEMENT_HIGHLIGHTED_STYLE)
        }
    }

    private fun createRepresentation(element: E): R {
        val representation = representationFactory(element)
        representation.styleClass.add(representationStyleClass)
        return representation
    }

    fun elementProperty(): SimpleListProperty<E> {
        return elements
    }

    fun highlightedElementsProperty(): SimpleSetProperty<E> {
        return highlightedElements
    }

    companion object {
        private const val ELEMENT_HIGHLIGHTED_STYLE = "highlighted"
    }
}
