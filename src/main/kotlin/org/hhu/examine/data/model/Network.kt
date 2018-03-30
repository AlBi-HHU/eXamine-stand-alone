package org.hhu.examine.data.model

import org.jgrapht.UndirectedGraph
import org.jgrapht.graph.Pseudograph

/** Network that wraps a graph with additional information. */
class Network(
        val dataSet: DataSet,
        val graph: UndirectedGraph<NetworkNode, NetworkLink>,
        val nodeAnnotations: Map<NetworkNode, Set<NetworkAnnotation>>
) {
    val annotations = nodeAnnotations.values.flatten().toSet()
}

fun emptyNetwork() = Network(
        emptyDataSet(),
        Pseudograph<NetworkNode, NetworkLink>(NetworkLink::class.java),
        emptyMap()
)

/** Induce a network from the given data set and for the given node. */
fun subNetworkByNodes(dataSet: DataSet, nodesToInclude: Collection<NetworkNode>): Network {

    // Induce graph.
    val graph = Pseudograph<NetworkNode, NetworkLink>(NetworkLink::class.java)
    nodesToInclude.forEach { graph.addVertex(it) }
    dataSet.links.rows.forEach { link ->
        if (nodesToInclude.contains(link.source) && nodesToInclude.contains(link.target))
            graph.addEdge(link.source, link.target, link)
    }

    // Induce annotations.
    val nodeToAnnotations = HashMap<NetworkNode, HashSet<NetworkAnnotation>>()
    dataSet.annotations.rows.forEach { annotation ->
        nodesToInclude.forEach { node ->
            if (annotation.nodes.contains(node))
                nodeToAnnotations.getOrPut(node, ::HashSet).add(annotation)
        }
    }

    return Network(dataSet, graph, nodeToAnnotations)
}

fun subNetworkByAnnotations(dataSet: DataSet, annotationsToInclude: Collection<NetworkAnnotation>): Network {
    val nodes = annotationsToInclude.flatMap { it.nodes }
    return subNetworkByNodes(dataSet, nodes)
}