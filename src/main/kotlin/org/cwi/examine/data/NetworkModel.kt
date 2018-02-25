package org.cwi.examine.data

import org.cwi.examine.math.extrema
import org.jgrapht.UndirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.Pseudograph
import org.jgrapht.graph.Subgraph
import org.jgrapht.graph.UndirectedSubgraph
import java.util.*
import java.util.Comparator.comparingDouble

/** Network that wraps a graph with additional information. */
class Network @JvmOverloads constructor(
        val graph: UndirectedGraph<NetworkNode, DefaultEdge> = Pseudograph(DefaultEdge::class.java),
        categories: List<NetworkAnnotationCategory> = ArrayList()) {

    val categories = categories.filter { it.name != "Module" }

    val annotations = categories
            .flatMap(NetworkAnnotationCategory::annotations)
            .associateBy(NetworkAnnotation::identifier)

    val modules = categories.find { it.name == "Module" } ?: NetworkAnnotationCategory("Module")

    val nodeScoreExtrema = extrema(graph.vertexSet().map(NetworkNode::score))
    val annotationScoreExtrema = extrema(annotations.values.map(NetworkAnnotation::score))

    /** Induce sub network from super network. */
    fun induce(nodesToInclude: Set<NetworkNode>): Network {

        // Verify whether entire subset is present in super network.
        nodesToInclude
                .filter { !graph.containsVertex(it) }
                .forEach { System.err.println("Sub network node $it not contained by super network.") }

        val subGraph = Subgraph(graph, nodesToInclude)
        val undirectedSubGraph = UndirectedSubgraph(graph, subGraph.vertexSet(), subGraph.edgeSet())
        val inducedCategories = categories.map { it.filterNodes(nodesToInclude::contains) }

        return Network(undirectedSubGraph, inducedCategories)
    }

    fun induce(categoryToInclude: NetworkAnnotationCategory): Network {
        val unionNodes = HashSet<NetworkNode>()
        categoryToInclude.annotations.forEach { unionNodes.addAll(it.nodes) }
        return induce(unionNodes)
    }

}

abstract class NetworkElement internal constructor(
        val identifier: String,
        val name: String,
        val url: String,
        val score: Double) {

    override fun toString() = name

    override fun equals(other: Any?) = other is NetworkElement && identifier == other.identifier

    override fun hashCode() = identifier.hashCode()

}

/** Represents a node of interest. */
class NetworkNode(id: String, name: String, url: String, score: Double) : NetworkElement(id, name, url, score) {

    val annotations: Set<NetworkAnnotation> = HashSet()

}

/** Category of network annotations. */
class NetworkAnnotationCategory(val name: String, annotations: List<NetworkAnnotation> = emptyList()) {

    val annotations: List<NetworkAnnotation> = annotations.sortedWith(
            comparingDouble(NetworkAnnotation::score).thenComparing(NetworkAnnotation::name)
    )

    /**
     * Filter annotations to only contain nodes that pass the given predicate.
     * Annotations that have no nodes left will be filtered out as well.
     */
    fun filterNodes(predicate: (NetworkNode) -> Boolean): NetworkAnnotationCategory {
        return NetworkAnnotationCategory(
                name,
                annotations.map { it.filterNodes(predicate) }.filter { it.nodes.isNotEmpty() })
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?) = other is NetworkAnnotationCategory && name == other.name

    override fun hashCode() = name.hashCode()

}

/** Annotation of a group of network nodes. */
class NetworkAnnotation(
        identifier: String,
        name: String,
        url: String,
        score: Double,
        val nodes: Set<NetworkNode>) : NetworkElement(identifier, name, url, score) {

    fun filterNodes(predicate: (NetworkNode) -> Boolean) =
            NetworkAnnotation(
                    identifier,
                    name,
                    url,
                    score,
                    nodes.filter(predicate).toSet())

}