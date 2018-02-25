package org.cwi.examine.main

import javafx.beans.binding.Bindings
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.property.*
import javafx.collections.FXCollections.*
import javafx.collections.ObservableList
import javafx.scene.paint.Color
import org.cwi.examine.data.*
import org.cwi.examine.visualization.color.BlueWhiteRed
import org.cwi.examine.visualization.color.ColormapInterval
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
    private val annotationWeights = SimpleMapProperty(observableHashMap<NetworkAnnotation, Double>())
    private val annotationColors = AnnotationColors()

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
        // Clear all selections, such that transition to new network is consistent.
        selectedAnnotations.clear()
        annotationWeights.clear()
        annotationColors.clear()
        activeCategories.clear()
        clearHighlights()

        activeDataSet = dataSetToActivate

        activeCategories.setAll(activeNetwork?.categories ?: emptyList())
    }

    /** Toggle the selected state of the given annotation. */
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

    /** Adjust the weight (importance) of the given annotation by the given weight change. */
    fun changeAnnotationWeight(annotation: NetworkAnnotation, weightChange: Double) {
        val currentWeight = annotationWeightsProperty()[annotation] ?: 1.0
        val newWeight = Math.max(1.0, currentWeight + weightChange)
        annotationWeightsProperty()[annotation] = newWeight
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

    fun annotationWeightsProperty(): ReadOnlyMapProperty<NetworkAnnotation, Double> = annotationWeights

    fun selectedAnnotationsProperty(): ReadOnlyListProperty<NetworkAnnotation> = selectedAnnotations

    fun annotationColorProperty(): ReadOnlyMapProperty<NetworkAnnotation, Color> = annotationColors.colorMapProperty()

    fun highlightedNodes(): ReadOnlySetProperty<NetworkNode> = highlightedNodes

    fun highlightedLinks(): ReadOnlySetProperty<DefaultEdge> = highlightedLinks

    fun highlightedAnnotations(): ReadOnlySetProperty<NetworkAnnotation> = highlightedAnnotations

    fun nodeColormap(): ReadOnlyObjectProperty<ColormapInterval?> = nodeColormap

    companion object {
        const val DATA_SET_DIRECTORY = "data-sets"
        const val DEFAULT_ANNOTATION_WEIGHT = 1.0
    }

}
