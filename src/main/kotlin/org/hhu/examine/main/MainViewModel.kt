package org.hhu.examine.main

import javafx.beans.binding.Bindings
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.property.*
import javafx.collections.FXCollections.*
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.paint.Color
import org.hhu.examine.data.*
import org.hhu.examine.visualization.color.BlueWhiteRed
import org.hhu.examine.visualization.color.ColormapInterval
import org.jgrapht.graph.DefaultEdge
import tornadofx.Controller
import tornadofx.getProperty
import tornadofx.property
import java.util.concurrent.Callable

/** View model of the main section. Maintains exploration state of a network that is being viewed. */
class MainViewModel : Controller() {

    val dataSets: ObservableList<DataSet> = unmodifiableObservableList(observableArrayList(
            listDataSets(DATA_SET_DIRECTORY)
    ))

    var activeDataSet: DataSet? by property()
        private set

    var activeNetwork: Network? by property(Network())
        private set

    val activeCategories: ObservableList<NetworkAnnotationCategory> = observableArrayList()

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val annotationColorModel = AnnotationColors()
    val annotationColors: ObservableMap<NetworkAnnotation, Color> = annotationColorModel.colorMap

    private val highlightedNodes = SimpleSetProperty(observableSet<NetworkNode>())
    private val highlightedLinks = SimpleSetProperty(observableSet<DefaultEdge>())
    private val highlightedAnnotations = SimpleSetProperty(observableSet<NetworkAnnotation>())

    private val nodeColormap: ObjectProperty<ColormapInterval?> = SimpleObjectProperty(null)

    init {
        // Induce active network from active data set.
        getProperty(MainViewModel::activeNetwork).bind(createObjectBinding(
                Callable { activeDataSet?.let { it.network.induce(it.network.modules) } },
                activeDataSetProperty()
        ))

        // Derive colormap for node scores.
        nodeColormap.bind(Bindings.createObjectBinding(Callable {
            val scoreExtrema = activeNetwork?.nodeScoreExtrema
            val symmetricScoreExtrema = scoreExtrema?.expandToCenter(0.0)
            symmetricScoreExtrema?.let { ColormapInterval(BlueWhiteRed, it) }
        }, activeNetworkProperty()))

        if (!dataSets.isEmpty()) activateDataSet(dataSets[0])
    }

    /** Set the data set that is being worked on. */
    fun activateDataSet(dataSetToActivate: DataSet) {

        // Annotations to preserve.
        val preservedIds = selectedAnnotations.map(NetworkAnnotation::identifier)
        val preservedIdToColor = annotationColorModel.colorMap.mapKeys { it.key.identifier }

        // Clear all selections, such that transition to new network is consistent.
        selectedAnnotations.clear()
        annotationColorModel.clear()
        activeCategories.clear()
        clearHighlights()

        // Switch to new data set.
        activeDataSet = dataSetToActivate
        activeCategories.setAll(activeNetwork?.categories ?: emptyList())

        // Restore previously selected annotations where possible for the new network.
        val activationAnnotations = dataSetToActivate.network.annotations
        selectedAnnotations.setAll(preservedIds.mapNotNull(activationAnnotations::get))
        annotationColorModel.putAllColors(preservedIdToColor
                .filterKeys(activationAnnotations::containsKey)
                .mapKeys { activationAnnotations.getValue(it.key) })
    }

    /** Toggle the selected state of the given annotation. */
    fun toggleAnnotation(annotation: NetworkAnnotation) {

        if (selectedAnnotations.contains(annotation)) {
            selectedAnnotationsProperty().remove(annotation)
            annotationColorModel.releaseColor(annotation)
        } else {
            annotationColorModel.assignColor(annotation)
            selectedAnnotationsProperty().add(annotation)
        }
    }

    /**
     * Highlight the given annotations, which also highlights the nodes and links
     * that are fully contained by individual annotations.
     */
    fun highlightAnnotations(annotations: List<NetworkAnnotation>) {
        clearHighlights()
        for (annotation in annotations) {
            highlightedNodes.addAll(annotation.nodes)
            highlightedAnnotations.add(annotation)
        }
        induceHighlightedLinks()
    }

    fun highlightNodes(nodes: List<NetworkNode>) {
        clearHighlights()
        highlightedNodes.addAll(nodes)
        induceHighlightedLinks()
        induceHighlightedAnnotations()
    }

    fun highlightLink(edges: List<DefaultEdge>) {
        clearHighlights()

        activeNetwork?.graph?.let { graph ->
            for (edge in edges) {
                highlightedNodes.add(graph.getEdgeSource(edge))
                highlightedNodes.add(graph.getEdgeTarget(edge))
                highlightedLinks.add(edge)
            }
        }

        induceHighlightedAnnotations()
    }

    private fun induceHighlightedLinks() {
        activeNetwork?.graph?.let { graph ->
            val linksToHighlight = graph.edgeSet()?.filter { edge ->
                highlightedNodes.contains(graph.getEdgeSource(edge)) &&
                        highlightedNodes.contains(graph.getEdgeTarget(edge))
            } ?: emptyList()
            highlightedLinks.addAll(linksToHighlight)
        }
    }

    private fun induceHighlightedAnnotations() {
        if (highlightedNodes.get().isNotEmpty()) {
            val annotationsToHighlight = activeNetwork?.annotations?.values
                    ?.filter { it.nodes.containsAll(highlightedNodes.get()) }
                    ?: emptyList()
            highlightedAnnotations.addAll(annotationsToHighlight)
        }
    }

    /** Clears the highlighted state of nodes, links, and contours. */
    private fun clearHighlights() {
        highlightedNodes.clear()
        highlightedLinks.clear()
        highlightedAnnotations.clear()
    }

    fun activeDataSetProperty(): ReadOnlyObjectProperty<DataSet?> = getProperty(MainViewModel::activeDataSet)

    fun activeNetworkProperty(): ReadOnlyObjectProperty<Network?> = getProperty(MainViewModel::activeNetwork)

    fun selectedAnnotationsProperty(): ReadOnlyListProperty<NetworkAnnotation> = selectedAnnotations

    fun highlightedNodes(): ReadOnlySetProperty<NetworkNode> = highlightedNodes

    fun highlightedLinks(): ReadOnlySetProperty<DefaultEdge> = highlightedLinks

    fun highlightedAnnotations(): ReadOnlySetProperty<NetworkAnnotation> = highlightedAnnotations

    fun nodeColormap(): ReadOnlyObjectProperty<ColormapInterval?> = nodeColormap

    companion object {
        const val DATA_SET_DIRECTORY = "data-sets"
    }

}