package org.hhu.examine.main

import javafx.beans.binding.Bindings
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.*
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.scene.paint.Color
import org.hhu.examine.data.csv.listDataSets
import org.hhu.examine.data.csv.readDataSet
import org.hhu.examine.data.model.*
import org.hhu.examine.data.table.extrema
import org.hhu.examine.property.bind
import org.hhu.examine.visualization.color.BlueWhiteRed
import org.hhu.examine.visualization.color.ColormapInterval
import tornadofx.Controller
import tornadofx.getProperty
import tornadofx.property
import java.awt.Desktop.isDesktopSupported
import java.io.File
import java.util.concurrent.Callable

const val DATA_SET_DIRECTORY = "data-sets"

/** View model of the main UI. Maintains the exploration state of a data set that is being viewed. */
class MainViewModel : Controller() {

    val dataSets: ObservableList<File> = unmodifiableObservableList(observableArrayList(
            listDataSets(DATA_SET_DIRECTORY)
    ))

    var dataSet: DataSet by property(emptyDataSet())
        private set

    var activeNetwork: Network by property(emptyNetwork())
        private set

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val annotationColorModel = AnnotationColors()
    val annotationColors: ObservableMap<NetworkAnnotation, Color> = annotationColorModel.colorMap

    var nodeColormapColumn: String? by property()
    private val nodeColormap = SimpleObjectProperty<ColormapInterval?>(null)

    private val hoveredRow = SimpleObjectProperty<NetworkRow?>()
    private val selectedRow = SimpleObjectProperty<NetworkRow?>()
    val highlightedRow: ObservableValue<NetworkRow?> = createObjectBinding(Callable {
        hoveredRow.value ?: selectedRow.value
    }, hoveredRow, selectedRow)

    val highlightedNodes: ObservableSet<NetworkNode> = observableSet()
    val highlightedLinks: ObservableSet<NetworkLink> = observableSet()
    val highlightedAnnotations: ObservableSet<NetworkAnnotation> = observableSet()

    init {
        nodeColormap.bind(Bindings.createObjectBinding(Callable {
            val scoreExtrema = dataSet.nodes.numberColumns[nodeColormapColumn ?: ""]
                    ?.extrema(activeNetwork.graph.vertexSet())
            val symmetricScoreExtrema = scoreExtrema?.expandToCenter(0.0)
            symmetricScoreExtrema?.let { ColormapInterval(BlueWhiteRed, it) }
        }, activeNetworkProperty(), nodeColormapColumnProperty()))

        if (dataSets.isNotEmpty()) activateDataSet(dataSets[0])

        // Highlight nodes based on the type of the selected row.
        highlightedNodes.bind(highlightedRow, ::extractNodes)

        // Highlight all links that contain all highlighted nodes.
        highlightedLinks.bind(highlightedRow, { row ->
            if (row is NetworkLink) {
                setOf(row)
            } else {
                val nodes = extractNodes(row)
                activeNetwork?.links?.filter {
                    nodes.isNotEmpty() and nodes.contains(it.source) and nodes.contains(it.target)
                }.rows.toSet()
            }
        })

        // Highlight all annotations that contain all highlighted nodes.
        highlightedAnnotations.bind(highlightedRow, { row ->
            if (row is NetworkAnnotation) {
                setOf(row)
            } else {
                val nodes = extractNodes(row)
                activeNetwork?.annotations?.filter {
                    nodes.isNotEmpty() and it.nodes.containsAll(nodes)
                }.rows.toSet()
            }
        })
    }

    private fun extractNodes(row: NetworkRow?) =
            when (row) {
                is NetworkNode -> setOf(row)
                is NetworkLink -> setOf(row.source, row.target)
                is NetworkAnnotation -> row.nodes
                else -> emptySet()
            }

    /** Set the data set that is being worked on. */
    fun activateDataSet(dataSetFile: File) {

        // Colormap column to preserver.
        val preservedNodeColormapColumn = nodeColormapColumn

        // Annotations to preserve.
        val preservedIdToColor = annotationColorModel.colorMap
                .mapKeys { (annotation, color) ->
                    dataSet.annotations.identities[annotation]
                }

        // Clear all selections, such that transition to new network is consistent.
        nodeColormapColumn = null
        selectedAnnotations.clear()
        annotationColorModel.clear()
        selectedRow.value = null

        // Transition to new data set and selected network.
        activeNetwork = emptyNetwork()
        dataSet = readDataSet(dataSetFile)
        activeNetwork = dataSet.induceFromAnnotations(dataSet.modules)

        // Restore colormap column.
        val numberColumns = activeNetwork.nodes.numberColumns.columns.keys
        nodeColormapColumn = if (numberColumns.contains(preservedNodeColormapColumn))
            preservedNodeColormapColumn
        else
            numberColumns.firstOrNull()

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
            selectedAnnotations.remove(annotation)
            annotationColorModel.releaseColor(annotation)
        } else {
            annotationColorModel.assignColor(annotation)
            selectedAnnotations.add(annotation)
        }
    }

    fun hover(row: NetworkRow?) {
        hoveredRow.value = row
    }

    fun select(row: NetworkRow?) {
        selectedRow.value = row
    }

    /** Open a web browser at the URL of the given element. */
    fun openBrowser(node: NetworkNode) {
        val hrefColumn = dataSet?.nodes?.hrefColumns["URL"]
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

    fun dataSetProperty(): ReadOnlyObjectProperty<DataSet> = getProperty(MainViewModel::dataSet)

    fun activeNetworkProperty(): ReadOnlyObjectProperty<Network> = getProperty(MainViewModel::activeNetwork)

    fun selectedAnnotationsProperty(): ReadOnlyListProperty<NetworkAnnotation> = selectedAnnotations

    fun nodeColormapColumnProperty(): ReadOnlyObjectProperty<String?> = getProperty(MainViewModel::nodeColormapColumn)

    fun nodeColormap(): ReadOnlyObjectProperty<ColormapInterval?> = nodeColormap

}