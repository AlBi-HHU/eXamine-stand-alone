package org.hhu.examine.main

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.collections.FXCollections.*
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.paint.Color
import org.hhu.examine.data.csv.listDataSets
import org.hhu.examine.data.csv.readDataSet
import org.hhu.examine.data.model.*
import org.hhu.examine.data.table.extrema
import org.hhu.examine.visualization.color.BlueWhiteRed
import org.hhu.examine.visualization.color.ColormapInterval
import tornadofx.Controller
import tornadofx.getProperty
import tornadofx.property
import java.awt.Desktop.isDesktopSupported
import java.io.File
import java.util.concurrent.Callable

/** View model of the main UI. Maintains the exploration state of a data set that is being viewed. */
class MainViewModel : Controller() {

    val dataSets: ObservableList<File> = unmodifiableObservableList(observableArrayList(
            listDataSets(DATA_SET_DIRECTORY)
    ))

    var activeNetwork: Network by property(emptyNetwork())
        private set

    val activeCategories: ObservableList<String> = observableArrayList()

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val annotationColorModel = AnnotationColors()
    val annotationColors: ObservableMap<NetworkAnnotation, Color> = annotationColorModel.colorMap

    private val highlightedNodes = SimpleSetProperty(observableSet<NetworkNode>())
    private val highlightedLinks = SimpleSetProperty(observableSet<NetworkLink>())
    private val highlightedAnnotations = SimpleSetProperty(observableSet<NetworkAnnotation>())

    private val nodeColormap: ObjectProperty<ColormapInterval?> = SimpleObjectProperty(null)

    init {
        // Derive colormap for node scores.
        nodeColormap.bind(Bindings.createObjectBinding(Callable {
            val scoreExtrema = activeNetwork.dataSet.nodes.numberColumns["Score"]?.extrema(activeNetwork.graph.vertexSet())
            val symmetricScoreExtrema = scoreExtrema?.expandToCenter(0.0)
            symmetricScoreExtrema?.let { ColormapInterval(BlueWhiteRed, it) }
        }, activeNetworkProperty()))

        if (dataSets.isNotEmpty()) activateDataSet(dataSets[0])
    }

    /** Set the data set that is being worked on. */
    fun activateDataSet(dataSetFile: File) {

        // Annotations to preserve.
        val preservedIdToColor = annotationColorModel.colorMap
                .mapKeys { (annotation, color) ->
                    activeNetwork.dataSet.annotations.identities[annotation]
                }

        // Clear all selections, such that transition to new network is consistent.
        selectedAnnotations.clear()
        annotationColorModel.clear()
        activeCategories.clear()
        clearHighlights()

        // Switch to new network.
        val dataSet = readDataSet(dataSetFile)
        activeNetwork = subNetworkByAnnotations(dataSet, dataSet.modules)
        activeCategories.setAll(dataSet.annotationCategories.keys)

        // Restore previously selected annotations where possible for the new network.
        val annotationsToColors = dataSet.annotations.rows
                .mapNotNull { annotation ->
                    val id = dataSet.annotations.identities[annotation]
                    val color = preservedIdToColor[id]
                    color?.let { Pair(annotation, color) }
                }.toMap()
        selectedAnnotations.setAll(annotationsToColors.keys)
        annotationColorModel.putAllColors(annotationsToColors)
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

    fun highlightLink(links: List<NetworkLink>) {
        clearHighlights()

        activeNetwork?.graph?.let { graph ->
            for (link in links) {
                highlightedNodes.add(graph.getEdgeSource(link))
                highlightedNodes.add(graph.getEdgeTarget(link))
                highlightedLinks.add(link)
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
            val annotationsToHighlight = activeNetwork?.annotations
                    ?.filter { it.nodes.containsAll(highlightedNodes.get()) }
            highlightedAnnotations.addAll(annotationsToHighlight)
        }
    }

    /** Clears the highlighted state of nodes, links, and contours. */
    private fun clearHighlights() {
        highlightedNodes.clear()
        highlightedLinks.clear()
        highlightedAnnotations.clear()
    }

    /** Open a web browser at the URL of the given element. */
    fun openBrowser(node: NetworkNode) {
        val hrefColumn = activeNetwork?.dataSet?.nodes?.hrefColumns["URL"]
        if (hrefColumn != null) hrefColumn[node]?.let(::openBrowser)
    }

    private fun openBrowser(url: String) {
        // Try regular show document, fall back to process creation for unix.
        try {
            hostServices.showDocument(url)
        } catch (ex: NoClassDefFoundError) {
            if (isDesktopSupported()) {
                ProcessBuilder("x-www-browser", url).start()
            }
        }
    }

    fun activeNetworkProperty(): ReadOnlyObjectProperty<Network> = getProperty(MainViewModel::activeNetwork)

    fun selectedAnnotationsProperty(): ReadOnlyListProperty<NetworkAnnotation> = selectedAnnotations

    fun highlightedNodes(): ReadOnlySetProperty<NetworkNode> = highlightedNodes

    fun highlightedLinks(): ReadOnlySetProperty<NetworkLink> = highlightedLinks

    fun highlightedAnnotations(): ReadOnlySetProperty<NetworkAnnotation> = highlightedAnnotations

    fun nodeColormap(): ReadOnlyObjectProperty<ColormapInterval?> = nodeColormap

    companion object {
        const val DATA_SET_DIRECTORY = "data-sets"
    }

}