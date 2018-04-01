package org.hhu.examine.data.model

import org.hhu.examine.data.table.Row
import org.jgrapht.UndirectedGraph
import org.jgrapht.graph.Pseudograph

class NetworkNode(override val index: Int) : Row

class NetworkLink(override val index: Int, val source: NetworkNode, val target: NetworkNode) : Row

class NetworkAnnotation(override val index: Int, val nodes: Set<NetworkNode>) : Row

interface Network {

    val nodes: DataTable<NetworkNode>

    val links: DataTable<NetworkLink>

    val annotations: DataTable<NetworkAnnotation>

    val graph: UndirectedGraph<NetworkNode, NetworkLink>

    fun induceFromNodes(nodesToSelect: List<NetworkNode>): Network {
        val nodeSet = nodesToSelect.toSet()

        return SimpleNetwork(
                nodes.select(nodesToSelect),
                links.filter { nodeSet.contains(it.source) && nodeSet.contains(it.target) },
                annotations.filter { it.nodes.any(nodeSet::contains) }
        )
    }

    fun induceFromAnnotations(annotationsToSelect: List<NetworkAnnotation>): Network =
            induceFromNodes(annotationsToSelect.flatMap(NetworkAnnotation::nodes).distinct())

}

fun emptyNetwork(): Network = SimpleNetwork(
        emptyDataTable(),
        emptyDataTable(),
        emptyDataTable()
)

private class SimpleNetwork(
        override val nodes: DataTable<NetworkNode>,
        override val links: DataTable<NetworkLink>,
        override val annotations: DataTable<NetworkAnnotation>
) : Network {

    override val graph by lazy { networkToGraph(this) }

}

/** Induce a graph from the given network. */
internal fun networkToGraph(network: Network): UndirectedGraph<NetworkNode, NetworkLink> {
    val graph = Pseudograph<NetworkNode, NetworkLink>(NetworkLink::class.java)
    network.nodes.rows.forEach { graph.addVertex(it) }
    network.links.rows.forEach { graph.addEdge(it.source, it.target, it) }
    return graph
}