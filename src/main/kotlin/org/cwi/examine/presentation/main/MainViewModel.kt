package org.cwi.examine.presentation.main

import javafx.beans.property.*
import javafx.collections.FXCollections.*
import javafx.scene.paint.Color
import org.cwi.examine.model.Network
import org.cwi.examine.model.NetworkAnnotation
import org.cwi.examine.model.NetworkCategory
import org.cwi.examine.model.NetworkNode
import org.jgrapht.graph.DefaultEdge

/**
 * View model of the main section. Maintains exploration state of a network that is being viewed.
 */
class MainViewModel {

    private val activeNetwork = SimpleObjectProperty(Network())

    val categories = observableArrayList<NetworkCategory>()

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val annotationWeights = SimpleMapProperty(observableHashMap<NetworkAnnotation, Double>())
    private val annotationColors = AnnotationColors()

    private val highlightedNodes = SimpleSetProperty(observableSet<NetworkNode>())
    private val highlightedLinks = SimpleSetProperty(observableSet<DefaultEdge>())
    private val highlightedAnnotations = SimpleSetProperty(observableSet<NetworkAnnotation>())

    // -- Actions.

    /**
     * Activate the given network as being explored. This clears the entire exploration state.
     *
     * @param network The network to activate as being explored.
     */
    fun activateNetwork(network: Network) {

        activeNetwork.set(network)
        categories.setAll(network.categories)
        selectedAnnotations.clear()
        annotationWeights.clear()
        annotationColors.clear()
        highlightedNodes.clear()
        highlightedLinks.clear()
        highlightedAnnotations.clear()
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
    }

    /**
     * Clears the highlighted state of nodes, links, and contours.
     */
    fun clearHighlights() {

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

    fun highlightedNodesProperty(): ReadOnlySetProperty<NetworkNode> {
        return highlightedNodes
    }

    fun highlightedLinksProperty(): ReadOnlySetProperty<DefaultEdge> {
        return highlightedLinks
    }

    fun highlightedAnnotationsProperty(): ReadOnlySetProperty<NetworkAnnotation> {
        return highlightedAnnotations
    }

    companion object {

        private val DEFAULT_ANNOTATION_WEIGHT = 1.0
    }

}
