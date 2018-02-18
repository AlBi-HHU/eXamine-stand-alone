package org.cwi.examine.main

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.collections.FXCollections.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import org.cwi.examine.data.*
import org.cwi.examine.visualization.color.BlueWhiteRed
import org.cwi.examine.visualization.color.ColormapInterval
import org.jgrapht.graph.DefaultEdge
import tornadofx.Controller
import java.util.concurrent.Callable

/**
 * View model of the main section. Maintains exploration state of a network that is being viewed.
 */
class MainViewModel : Controller() {

    private val activeNetwork = SimpleObjectProperty(Network())

    val categories: ObservableList<NetworkCategory> = observableArrayList<NetworkCategory>()

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val annotationWeights = SimpleMapProperty(observableHashMap<NetworkAnnotation, Double>())
    private val annotationColors = AnnotationColors()

    private val highlightedNodes = SimpleSetProperty(observableSet<NetworkNode>())
    private val highlightedLinks = SimpleSetProperty(observableSet<DefaultEdge>())
    private val highlightedAnnotations = SimpleSetProperty(observableSet<NetworkAnnotation>())

    private val nodeColormap: ObjectProperty<ColormapInterval?> = SimpleObjectProperty(null)

    init {
        nodeColormap.bind(Bindings.createObjectBinding(Callable {
            val scoreExtrema = activeNetwork.get().nodeScoreExtrema
            val symmetricScoreExtrema = scoreExtrema?.expandToCenter(0.0)
            symmetricScoreExtrema?.let { ColormapInterval(BlueWhiteRed, it) }
        }, activeNetwork))

        val superNetwork = NetworkReader(CSV_FILE_PATH).readNetwork()
        val moduleNetwork = superNetwork.induce(superNetwork.modules)
        activateNetwork(moduleNetwork)
    }

    // -- Actions.

    /**
     * Activate the given network as being explored. This clears the entire exploration state.
     *
     * @param network The network to activate as being explored.
     */
    fun activateNetwork(network: Network) {
        selectedAnnotations.clear()
        annotationWeights.clear()
        annotationColors.clear()
        clearHighlights()

        activeNetwork.set(network)
        categories.setAll(network.categories)
    }

    /**
     * Toggle the selected state of the given annotation.
     *
     * @param annotation The annotation to toggle the selected state for.
     */
    fun toggleAnnotation(annotation: NetworkAnnotation) {

        if (selectedAnnotations.contains(annotation)) {
            selectedAnnotationsProperty().remove(annotation)
            annotationWeightsProperty().remove(annotation)
            annotationColors.releaseColor(annotation)
        } else {
            annotationColors.assignColor(annotation)
            annotationWeightsProperty()[annotation] = DEFAULT_ANNOTATION_WEIGHT
            selectedAnnotationsProperty().add(annotation)
        }
    }

    /**
     * Adjust the weight (importance) of the given annotation by the given weight change.
     *
     * @param annotation   The annotation to change the weight of.
     * @param weightChange The number by which to change the weight.
     */
    fun changeAnnotationWeight(annotation: NetworkAnnotation, weightChange: Double) {
        val currentWeight = annotationWeightsProperty()[annotation] ?: 1.0
        val newWeight = Math.max(1.0, currentWeight + weightChange)
        annotationWeightsProperty()[annotation] = newWeight
    }

    /**
     * Highlight the given annotations, which also highlights the nodes and links
     * that are fully contained by individual annotations.
     *
     * @param annotations The annotation to highlight.
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
        var graph = activeNetwork.get().graph

        clearHighlights()
        for (edge in edges) {
            highlightedNodes.add(graph.getEdgeSource(edge))
            highlightedNodes.add(graph.getEdgeTarget(edge))
            highlightedLinks.add(edge)
        }
        induceHighlightedAnnotations()
    }

    private fun induceHighlightedLinks() {
        val graph = activeNetwork.get().graph
        highlightedLinks.addAll(graph.edgeSet()
                .filter { edge ->
                    highlightedNodes.contains(graph.getEdgeSource(edge)) &&
                            highlightedNodes.contains(graph.getEdgeTarget(edge))
                }.toSet())
    }

    private fun induceHighlightedAnnotations() {
        val network = activeNetwork.get()

        if (highlightedNodes.get().isNotEmpty()) {
            highlightedAnnotations.addAll(network.annotations.filter { it.nodes.containsAll(highlightedNodes.get()) }.toSet())
        }
    }

    /** Clears the highlighted state of nodes, links, and contours. */
    private fun clearHighlights() {
        highlightedNodes.clear()
        highlightedLinks.clear()
        highlightedAnnotations.clear()
    }

    // -- Accessors.

    fun activeNetworkProperty(): ReadOnlyObjectProperty<Network> {
        return activeNetwork
    }

    fun annotationWeightsProperty(): ReadOnlyMapProperty<NetworkAnnotation, Double> {
        return annotationWeights
    }

    fun selectedAnnotationsProperty(): ReadOnlyListProperty<NetworkAnnotation> {
        return selectedAnnotations
    }

    fun annotationColorProperty(): ReadOnlyMapProperty<NetworkAnnotation, Color> {
        return annotationColors.colorMapProperty()
    }

    fun highlightedNodes(): ReadOnlySetProperty<NetworkNode> {
        return highlightedNodes
    }

    fun highlightedLinks(): ReadOnlySetProperty<DefaultEdge> {
        return highlightedLinks
    }

    fun highlightedAnnotations(): ReadOnlySetProperty<NetworkAnnotation> {
        return highlightedAnnotations
    }

    fun nodeColormap(): ReadOnlyObjectProperty<ColormapInterval?> {
        return nodeColormap
    }

    companion object {
        private const val CSV_FILE_PATH = "data/"
        private const val DEFAULT_ANNOTATION_WEIGHT = 1.0
    }

}
