package org.hhu.examine.main.nodelinkcontour

import javafx.beans.binding.Bindings.*
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import org.hhu.examine.data.model.Network
import org.hhu.examine.data.model.NetworkAnnotation
import org.hhu.examine.data.model.NetworkLink
import org.hhu.examine.data.model.NetworkNode
import org.hhu.examine.main.MainViewModel
import org.hhu.examine.main.nodelinkcontour.layout.Contours
import org.hhu.examine.main.nodelinkcontour.layout.Layout
import tornadofx.*
import java.util.*
import java.util.Arrays.asList
import java.util.concurrent.Callable
import java.util.stream.Collectors.toMap
import kotlin.collections.set

/**
 * Node, link, and contour depiction of a network with annotations.
 */
class NodeLinkContourView(private val model: MainViewModel) : ScrollPane() {

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())

    private var layout: Layout? = null
    private val nodePositions = observableHashMap<NetworkNode, Point2D>()
    private val linkPositions = observableHashMap<NetworkLink, Array<Point2D>>()

    private val contourLayer = ContourLayer()
    private val linkLayer = NetworkElementLayer("network-link", this::createLinkRepresentation)
    private val nodeLayer = NetworkElementLayer("network-node", this::createNodeRepresentation)

    private val layerGroup = Group(contourLayer, linkLayer, nodeLayer)
    private val layerPane = stackpane {
        children += layerGroup

        minWidth = USE_PREF_SIZE
        minHeight = USE_PREF_SIZE
        maxWidth = Region.USE_PREF_SIZE
        maxHeight = Region.USE_PREF_SIZE

        style {
            padding = box(1.em)
        }
    }

    init {
        styleClass.add("node-link-contour-view")

        isFitToHeight = true
        isFitToWidth = true
        content = BorderPane(layerPane)

        model.activeNetworkProperty().addListener { _, oldNetwork, newNetwork ->
            onNetworkChange(oldNetwork, newNetwork)
        }
        onNetworkChange(model.activeNetwork, model.activeNetwork)

        selectedAnnotations.addListener(ListChangeListener { _ -> updateLayout(model.activeNetwork, model.activeNetwork) })
        contourLayer.annotationsProperty().bind(selectedAnnotations)

        selectedAnnotations.bind(model.selectedAnnotationsProperty())
        contourLayer.colorsProperty().set(model.annotationColors)
        nodeLayer.highlightedElementsProperty().bind(model.highlightedNodes())
        linkLayer.highlightedElementsProperty().bind(model.highlightedLinks())
    }

    private fun onNetworkChange(oldNetwork: Network?, newNetwork: Network?) {
        updateLayout(oldNetwork, newNetwork)
        linkLayer.elementProperty().setAll(newNetwork?.graph?.edgeSet() ?: emptyList())
        nodeLayer.elementProperty().setAll(newNetwork?.graph?.vertexSet() ?: emptyList())
    }

    private fun updateLayout(oldNetwork: Network?, newNetwork: Network?) {

        if (newNetwork == null || oldNetwork != newNetwork) {
            layout = null
        } else {
            val nodeLabels = model.activeNetwork.graph.vertexSet().mapNotNull { node ->
                model.activeNetwork.dataSet.nodes.stringColumns["Symbol"]?.get(node)?.let { Pair(node, it) }
            }.toMap()
            layout = Layout(newNetwork, nodeLabels, selectedAnnotations, layout)

            val newPositions = HashMap<NetworkNode, Point2D>()
            newNetwork.graph.vertexSet().forEach { node -> newPositions[node] = layout!!.position(node) }
            nodePositions.clear()
            nodePositions.putAll(newPositions)

            linkPositions.clear()
            linkPositions.putAll(layout!!.linkPositions())

            contourLayer.contoursProperty().clear()
            contourLayer.contoursProperty().putAll(selectedAnnotations.stream()
                    .map { annotation -> Contours(annotation, layout!!) }
                    .collect(toMap(Contours::annotation, { it })))
        }
    }

    private fun createLinkRepresentation(link: NetworkLink): Node {
        val representation = LinkRepresentation()
        representation.controlPointsProperty().bind(createObjectBinding(
                Callable { FXCollections.observableList(asList(*linkPositions.getOrDefault(link, arrayOf()))) },
                linkPositions))

        // Highlight on hover.
        representation.onMouseEntered = EventHandler { _ -> model.highlightLink(asList(link)) }
        representation.onMouseExited = EventHandler { _ -> model.highlightLink(emptyList()) }

        return representation
    }

    private fun createNodeRepresentation(node: NetworkNode): Node {
        val enclosedDataSet = model.activeNetwork.dataSet

        val label = Label(enclosedDataSet.nodes.stringColumns["Symbol"]?.get(node))

        // Bind coordinate to node layout.
        label.layoutXProperty().bind(bindNodeX(node))
        label.layoutYProperty().bind(bindNodeY(node))

        // Translate label to bring its layout coordinate to its center.
        label.translateXProperty().bind(label.widthProperty().multiply(-.5))
        label.translateYProperty().bind(label.heightProperty().multiply(-.5))

        // Highlight on hover.
        label.onMouseEntered = EventHandler { _ -> model.highlightNodes(asList(node)) }
        label.onMouseExited = EventHandler { _ -> model.highlightNodes(emptyList()) }

        label.styleProperty().bind(createStringBinding(Callable {
            val colormap = model.nodeColormap().get()
            if (colormap == null)
                ""
            else
                "-fx-border-color: " + colormap(enclosedDataSet.nodes.numberColumns["Score"]?.get(node)).css
        }, model.nodeColormap()))

        // If node has an associated URL, navigate to it.
        label.onMouseClicked = EventHandler { _ -> model.openBrowser(node) }

        return label
    }

    private fun bindNodeX(node: NetworkNode) =
            createDoubleBinding(Callable { nodePositions.getOrDefault(node, Point2D.ZERO).x }, nodePositions)

    private fun bindNodeY(node: NetworkNode) =
            createDoubleBinding(Callable { nodePositions.getOrDefault(node, Point2D.ZERO).y }, nodePositions)

    fun getImageExportableRegion(): Region = layerPane

    override fun getUserAgentStylesheet(): String =
            javaClass.getResource("NodeLinkContourView.css").toExternalForm()

}
