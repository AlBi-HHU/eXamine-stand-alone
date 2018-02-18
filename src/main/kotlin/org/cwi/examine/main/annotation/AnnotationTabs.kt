package org.cwi.examine.main.annotation

import javafx.beans.property.*
import javafx.collections.FXCollections.*
import javafx.collections.ListChangeListener
import javafx.scene.control.TabPane
import javafx.scene.paint.Color
import org.cwi.examine.data.NetworkAnnotation
import org.cwi.examine.data.NetworkCategory
import java.util.function.Consumer
import java.util.stream.Collectors.toList

class AnnotationTabs : TabPane() {

    private val categories = SimpleListProperty(observableArrayList<NetworkCategory>())
    private val annotationColors = SimpleMapProperty(observableHashMap<NetworkAnnotation, Color>())
    private val highlightedAnnotations = SimpleSetProperty(observableSet<NetworkAnnotation>())

    private val onToggleAnnotation = SimpleObjectProperty<Consumer<NetworkAnnotation>>(Consumer { _ -> })
    private val onHighlightAnnotations = SimpleObjectProperty<Consumer<List<NetworkAnnotation>>>(Consumer { _ -> })

    init {
        styleClass.add("annotation-tabs")

        categories.addListener(ListChangeListener { this.onCategoriesChange(it) })
    }

    private fun onCategoriesChange(change: ListChangeListener.Change<out NetworkCategory>) {

        val tabs = categories.stream()
                .map<AnnotationTab>({ this.createAndBindTab(it) })
                .collect(toList())
        getTabs().setAll(tabs)
    }

    private fun createAndBindTab(category: NetworkCategory): AnnotationTab {

        val tab = AnnotationTab(category)
        tab.annotationColorsProperty().bind(annotationColors)
        tab.highlightedAnnotationsProperty().bind(highlightedAnnotations)
        tab.onToggleAnnotationProperty().bind(onToggleAnnotation)
        tab.onHighlightAnnotationsProperty().bind(onHighlightAnnotations)

        return tab
    }

    fun categoriesProperty(): ListProperty<NetworkCategory> {
        return categories
    }

    fun annotationColorsProperty(): MapProperty<NetworkAnnotation, Color> {
        return annotationColors
    }

    fun highlightedAnnotationsProperty(): SetProperty<NetworkAnnotation> {
        return highlightedAnnotations
    }

    fun onToggleAnnotationProperty(): SimpleObjectProperty<Consumer<NetworkAnnotation>> {
        return onToggleAnnotation
    }

    fun onHighlightAnnotationsProperty(): SimpleObjectProperty<Consumer<List<NetworkAnnotation>>> {
        return onHighlightAnnotations
    }

    override fun getUserAgentStylesheet(): String {
        return AnnotationTabs::class.java.getResource("AnnotationTabs.css").toExternalForm()
    }
}
