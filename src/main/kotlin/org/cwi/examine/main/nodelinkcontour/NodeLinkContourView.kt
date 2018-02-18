package org.cwi.examine.main.nodelinkcontour

import javafx.beans.binding.Bindings.*
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.collections.FXCollections
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import org.cwi.examine.data.Network
import org.cwi.examine.data.NetworkAnnotation
import org.cwi.examine.data.NetworkNode
import org.cwi.examine.main.MainViewModel
import org.cwi.examine.main.nodelinkcontour.layout.Contours
import org.cwi.examine.main.nodelinkcontour.layout.Layout
import org.jgrapht.graph.DefaultEdge
import tornadofx.css
import java.util.*
import java.util.Arrays.asList
import java.util.concurrent.Callable
import java.util.stream.Collectors

/**
 * Node, link, and contour depiction of a network with annotations.
 */
class NodeLinkContourView(private val model: MainViewModel) : ScrollPane() {

    private val selectedAnnotations = SimpleListProperty(observableArrayList<NetworkAnnotation>())
    private val annotationWeights = SimpleMapProperty(observableHashMap<NetworkAnnotation, Double>())

    private var layout: Layout? = null
    private val nodePositions = observableHashMap<NetworkNode, Point2D>()
    private val linkPositions = observableHashMap<DefaultEdge, Array<Point2D>>()

    private val contourLayer = ContourLayer()
    private val linkLayer = NetworkElementLayer("network-link", this::createLinkRepresentation)
    private val nodeLayer = NetworkElementLayer("network-node", this::createNodeRepresentation)
    private val layerStack = Group(contourLayer, linkLayer, nodeLayer)

    init {
        styleClass.add("node-link-contour-view")

        val layerContainer = BorderPane(layerStack)
        BorderPane.setAlignment(layerStack, Pos.CENTER)
        isFitToHeight = true
        isFitToWidth = true
        content = layerContainer

        model.activeNetworkProperty().addListener({ _, _, network -> onNetworkChange(network) })
        onNetworkChange(model.activeNetworkProperty().get())

        selectedAnnotations.addListener(ListChangeListener { _ -> updateLayout() })
        contourLayer.annotationsProperty().bind(selectedAnnotations)

        selectedAnnotations.bind(model.selectedAnnotationsProperty())
        annotationWeights.bind(model.annotationWeightsProperty())
        contourLayer.colorsProperty().bind(model.annotationColorProperty())
        nodeLayer.highlightedElementsProperty().bind(model.highlightedNodes())
        linkLayer.highlightedElementsProperty().bind(model.highlightedLinks())
    }

    private fun onNetworkChange(network: Network) {
        updateLayout()
        linkLayer.elementProperty().setAll(network.graph.edgeSet())
        nodeLayer.elementProperty().setAll(network.graph.vertexSet())
    }

    private fun updateLayout() {

        if (model.activeNetworkProperty() == null) {
            layout = null
        } else {
            layout = Layout(model.activeNetworkProperty().get(), annotationWeights, layout)

            val newPositions = HashMap<NetworkNode, Point2D>()
            model.activeNetworkProperty().get().graph.vertexSet().forEach { node -> newPositions[node] = layout!!.position(node) }
            nodePositions.clear()
            nodePositions.putAll(newPositions)

            linkPositions.clear()
            linkPositions.putAll(layout!!.linkPositions())

            contourLayer.contoursProperty().clear()
            contourLayer.contoursProperty().putAll(selectedAnnotations.stream()
                    .map { annotation -> Contours(annotation, layout!!) }
                    .collect(Collectors.toMap(Contours::annotation, { it })))
        }
    }

    private fun createLinkRepresentation(edge: DefaultEdge): Node {
        val link = LinkRepresentation()
        link.controlPointsProperty().bind(createObjectBinding(
                Callable { FXCollections.observableList(asList(*linkPositions.getOrDefault(edge, arrayOf()))) },
                linkPositions))

        // Highlight on hover.
        link.onMouseEntered = EventHandler { _ -> model.highlightLink(asList(edge)) }
        link.onMouseExited = EventHandler { _ -> model.highlightLink(emptyList()) }

        return link
    }

    private fun createNodeRepresentation(node: NetworkNode): Node {
        val label = Label(node.name)

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
                "-fx-border-color: " + colormap(node.score).css
        }, model.nodeColormap()))

        return label
    }

    private fun bindNodeX(node: NetworkNode): DoubleBinding {
        return createDoubleBinding(Callable { nodePositions.getOrDefault(node, Point2D.ZERO).x }, nodePositions)
    }

    private fun bindNodeY(node: NetworkNode): DoubleBinding {
        return createDoubleBinding(Callable { nodePositions.getOrDefault(node, Point2D.ZERO).y }, nodePositions)
    }

    override fun getUserAgentStylesheet(): String =
            javaClass.getResource("NodeLinkContourView.css").toExternalForm()
}
