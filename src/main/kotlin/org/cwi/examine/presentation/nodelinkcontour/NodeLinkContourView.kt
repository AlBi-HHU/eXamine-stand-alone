package org.cwi.examine.presentation.nodelinkcontour

import javafx.beans.binding.Bindings
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import org.cwi.examine.model.Network
import org.cwi.examine.model.NetworkAnnotation
import org.cwi.examine.model.NetworkNode
import org.cwi.examine.presentation.nodelinkcontour.layout.Contours
import org.cwi.examine.presentation.nodelinkcontour.layout.Layout
import org.jgrapht.graph.DefaultEdge
import java.util.*
import java.util.Arrays.asList
import java.util.concurrent.Callable
import java.util.stream.Collectors

/**
 * Node, link, and contour depiction of a network with annotations.
 */
class NodeLinkContourView : ScrollPane() {

    private val network = SimpleObjectProperty<Network>()
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

        network.addListener({ observable, old, network -> this.onNetworkChange(network) })
        selectedAnnotationsProperty().addListener(ListChangeListener { _ -> updateLayout() })
        contourLayer.annotationsProperty().bind(selectedAnnotations)
    }

    private fun onNetworkChange(
            network: Network) {

        updateLayout()
        linkLayer.elementProperty().setAll(network.graph.edgeSet())
        nodeLayer.elementProperty().setAll(network.graph.vertexSet())
    }

    private fun updateLayout() {

        if (network == null) {
            layout = null
        } else {
            layout = Layout(network.get(), annotationWeights, layout)

            val newPositions = HashMap<NetworkNode, Point2D>()
            networkProperty().get().graph.vertexSet().forEach { node -> newPositions[node] = layout!!.position(node) }
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

        val representation = LinkRepresentation(edge)
        representation.controlPointsProperty().bind(createObjectBinding<ObservableList<Point2D>>(
                Callable { FXCollections.observableList(asList(*(linkPositions as java.util.Map<DefaultEdge, Array<Point2D>>).getOrDefault(edge, arrayOf()))) },
                linkPositions))

        return representation
    }

    private fun createNodeRepresentation(node: NetworkNode): Node {

        val label = Label(node.name)

        // Bind coordinate to node layout.
        label.layoutXProperty().bind(bindNodeX(node))
        label.layoutYProperty().bind(bindNodeY(node))

        // Translate label to bring its layout coordinate to its center.
        label.translateXProperty().bind(label.widthProperty().multiply(-.5))
        label.translateYProperty().bind(label.heightProperty().multiply(-.5))

        return label
    }

    private fun bindNodeX(node: NetworkNode): DoubleBinding {
        return Bindings.createDoubleBinding(Callable { (nodePositions as java.util.Map<NetworkNode, Point2D>).getOrDefault(node, Point2D.ZERO).x }, nodePositions)
    }

    private fun bindNodeY(node: NetworkNode): DoubleBinding {
        return Bindings.createDoubleBinding(Callable { (nodePositions as java.util.Map<NetworkNode, Point2D>).getOrDefault(node, Point2D.ZERO).y }, nodePositions)
    }

    fun networkProperty(): ObjectProperty<Network> {
        return network
    }

    fun selectedAnnotationsProperty(): ListProperty<NetworkAnnotation> {
        return selectedAnnotations
    }

    fun annotationWeightsProperty(): MapProperty<NetworkAnnotation, Double> {
        return annotationWeights
    }

    fun annotationColorsProperty(): MapProperty<NetworkAnnotation, Color> {
        return contourLayer.colorsProperty()
    }

    fun highlightedNodesProperty(): SetProperty<NetworkNode> {
        return nodeLayer.highlightedElementsProperty()
    }

    fun highlightedLinksProperty(): SetProperty<DefaultEdge> {
        return linkLayer.highlightedElementsProperty()
    }

    override fun getUserAgentStylesheet(): String {
        return NodeLinkContourView::class.java.getResource("NodeLinkContourView.css").toExternalForm()
    }
}
