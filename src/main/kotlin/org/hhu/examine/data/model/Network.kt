package org.hhu.examine.data.model

import org.jgrapht.UndirectedGraph
import org.jgrapht.graph.Pseudograph

interface Network {

    val nodes: NetworkTable<NetworkNode>

    val links: NetworkTable<NetworkLink>

    val annotations: NetworkTable<NetworkAnnotation>

    val graph: UndirectedGraph<NetworkNode, NetworkLink>

    fun induceFromNodes(nodesToSelect: List<NetworkNode>): Network {
        val nodeSet = nodesToSelect.toSet()

        return SimpleNetwork(
                nodes.select(nodesToSelect),
                links.filter { nodeSet.contains(it.source) && nodeSet.contains(it.target) },
                annotations
                        .filter { it.nodes.any(nodeSet::contains) }
                        .map { NetworkAnnotation(it.index, it.nodes.filter(nodeSet::contains).toSet()) }
        )
    }

    fun induceFromAnnotations(annotationsToSelect: List<NetworkAnnotation>): Network =
            induceFromNodes(annotationsToSelect.flatMap(NetworkAnnotation::nodes).distinct())

}

fun emptyNetwork(): Network = SimpleNetwork(
        emptyNetworkTable(),
        emptyNetworkTable(),
        emptyNetworkTable()
)

private class SimpleNetwork(
        override val nodes: NetworkTable<NetworkNode>,
        override val links: NetworkTable<NetworkLink>,
        override val annotations: NetworkTable<NetworkAnnotation>
) : Network {

    override val graph by lazy { networkToGraph(this) }

}

/** Induce a graph from the given network. */
internal fun networkToGraph(network: Network): UndirectedGraph<NetworkNode, NetworkLink> {
    val graph = Pseudograph<NetworkNode, NetworkLink>(NetworkLink::class.java)
    network.nodes.rows.forEach { graph.addVertex(it) }
    network.links.rows.forEach { if (!graph.containsEdge(it.source, it.target)) graph.addEdge(it.source, it.target, it) }
    return graph
}
