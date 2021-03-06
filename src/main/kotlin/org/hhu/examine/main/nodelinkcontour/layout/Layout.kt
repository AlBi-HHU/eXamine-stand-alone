package org.hhu.examine.main.nodelinkcontour.layout

import javafx.geometry.Point2D
import javafx.scene.text.Text
import org.hhu.examine.data.model.Network
import org.hhu.examine.data.model.NetworkAnnotation
import org.hhu.examine.data.model.NetworkLink
import org.hhu.examine.data.model.NetworkNode
import org.hhu.examine.main.nodelinkcontour.layout.dwyer.cola.Descent
import org.hhu.examine.main.nodelinkcontour.layout.dwyer.vpsc.Constraint
import org.hhu.examine.main.nodelinkcontour.layout.dwyer.vpsc.Solver
import org.hhu.examine.main.nodelinkcontour.layout.dwyer.vpsc.Variable
import org.jgrapht.Graph
import org.jgrapht.WeightedGraph
import org.jgrapht.alg.FloydWarshallShortestPaths
import org.jgrapht.alg.PrimMinimumSpanningTree
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleGraph
import org.jgrapht.graph.SimpleWeightedGraph
import java.util.*
import java.util.Comparator.comparingInt

class Layout(
        val network: Network,
        val nodeLabels: Map<NetworkNode, String>,
        selectedAnnotations: List<NetworkAnnotation>,
        oldLayout: Layout?) {

    private val sets: MutableList<NetworkAnnotation> = ArrayList()
    val nodes: Array<NetworkNode>
    val nodeMemberships: MutableMap<NetworkNode, MutableList<NetworkAnnotation>> = HashMap()

    // Spanning set graphs.
    private var minDistGraph: WeightedGraph<NetworkNode, DefaultEdge>? = null
    private var spanGraphs: MutableList<Graph<NetworkNode, DefaultEdge>>? = null
    var richGraph: WeightedGraph<RichNode, RichEdge>? = null
    private var extRichGraph: WeightedGraph<RichNode, RichEdge>? = null
    private var richNodes: Array<RichNode?>? = null

    // Descent layout.
    private var index: MutableMap<NetworkNode, Int>? = null
    private var richIndex: MutableMap<RichNode, Int>? = null
    private var baseDilations: DoubleArray? = null
    private var radii: DoubleArray? = null
    private var mD: Array<DoubleArray>? = null
    private var P: Array<DoubleArray>? = null
    private var D: Array<DoubleArray>? = null
    private var G: Array<DoubleArray>? = null
    private var descent: Descent? = null

    init {

        // Order annotations by size.
        this.sets.addAll(selectedAnnotations)
        Collections.sort(this.sets, comparingInt { s2 -> s2.nodes.size })

        // Invert set membership for vertices.
        this.nodes = network.graph.vertexSet().toTypedArray()
        for (n in nodes) {
            nodeMemberships[n] = ArrayList()
        }
        for (s in sets) {
            for (n in s.nodes) {
                nodeMemberships[n]?.add(s)
            }
        }

        updatePositions(oldLayout)
    }

    fun updatePositions(): Boolean {
        return updatePositions(null)
    }

    private fun updatePositions(oldLayout: Layout?): Boolean {
        val converged: Boolean
        var vN = nodes.size

        if (index == null) {
            index = HashMap()
            for (i in 0 until vN) index!![nodes[i]] = i

            // Vertex line radii (width / 2) and base dilations (based on bounds height).
            baseDilations = DoubleArray(vN)
            radii = DoubleArray(vN)
            for (i in 0 until vN) {
                baseDilations!![i] = 0.5 * labelSpacedDimensions(nodeLabels[nodes[i]] ?: "").y
                radii!![i] = 0.5 * labelSpacedDimensions(nodeLabels[nodes[i]] ?: "").x
            }

            // Vertex to vertex minimum distance (based on set memberships).
            mD = Array(vN) { DoubleArray(vN) }
            for (i in 0 until vN) {
                val dil1 = baseDilations!![i]
                for (j in i + 1 until vN) {
                    val dil2 = baseDilations!![j]
                    mD!![j][i] = dil1 + dil2 + 2 * NODE_SPACE +
                            RIBBON_EXTENT * membershipDiscrepancy(nodes[i], nodes[j])
                    mD!![i][j] = mD!![j][i]
                }
            }

            // Construct set spanning graphs.
            initializeSetGraphs()

            // Update shortest path matrix to rich graph.
            vN = richNodes!!.size
            val paths = FloydWarshallShortestPaths(extRichGraph!!)
            D = Array(vN) { DoubleArray(vN) }
            for (i in 0 until vN)
                for (j in i + 1 until vN) {
                    D!![j][i] = paths.shortestDistance(richNodes!![i], richNodes!![j])
                    D!![i][j] = D!![j][i]
                }

            // Vertex positions start at (0,0), or at position of previous layout.
            P = Array(2) { DoubleArray(vN) }
            for (i in nodes.indices) {
                val pos = oldLayout?.position(richNodes!![i]) ?: Point2D(0.0, 0.0)
                P!![0][i] = pos.x
                P!![1][i] = pos.y
            }

            // Gradient descent.
            G = Array(vN) { DoubleArray(vN) }
            for (i in 0 until vN)
                for (j in i until vN) {
                    G!![j][i] = (if (extRichGraph!!.containsEdge(richNodes!![i], richNodes!![j]) || network.graph.containsEdge(richNodes!![i]!!.element, richNodes!![j]!!.element))
                        1
                    else
                        2).toDouble()
                    G!![i][j] = G!![j][i]
                }
            descent = Descent(P!!, D, null)

            // Apply initialIterations without user constraints or non-overlap constraints.
            descent!!.run(INITIAL_ITERATIONS)

            // Initialize vertex and contour bound respecting projection.
            // TODO: convert to rich graph form.
            descent!!.project = BoundProjection(radii!!, mD!!).projectFunctions()

            // Allow not immediately connected (by direction) nodes to relax apart (p-stress).
            descent!!.G = G
            descent!!.run(PHASE_ITERATIONS)

            converged = false
        } else {
            converged = descent!!.run(PHASE_ITERATIONS)
        }// Improve layout.

        // Measure span and shift nodes top left to (0,0).
        var minX = java.lang.Double.MAX_VALUE
        var minY = java.lang.Double.MAX_VALUE
        var maxX = java.lang.Double.MIN_VALUE
        var maxY = java.lang.Double.MIN_VALUE
        for (i in 0 until vN) {
            minX = Math.min(minX, P!![0][i])
            minY = Math.min(minY, P!![1][i])
            maxX = Math.max(maxX, P!![0][i])
            maxY = Math.max(maxY, P!![1][i])
        }

        for (i in 0 until vN) {
            P!![0][i] -= minX
            P!![1][i] -= minY
        }

        return converged
    }

    // Position of the given node, (0,0) iff null.
    fun position(node: NetworkNode?): Point2D {
        val result: Point2D

        result = if (index == null) {
            Point2D(0.0, 0.0)
        } else {
            val i = index!![node]
            if (i == null) Point2D(0.0, 0.0) else Point2D(P!![0][i], P!![1][i])
        }

        return result
    }

    // Position of the given node, (0,0) iff null.
    fun position(node: RichNode?): Point2D {
        val result: Point2D

        result = if (richIndex == null) {
            Point2D.ZERO
        } else {
            val i = richIndex!![node]
            if (i == null) Point2D.ZERO else Point2D(P!![0][i], P!![1][i])
        }

        return result
    }

    fun linkPositions(): Map<NetworkLink?, Array<Point2D>> {

        val positionMap = HashMap<NetworkLink?, Array<Point2D>>()

        for (richEdge in richGraph!!.edgeSet()) {

            val positions = Array<Point2D>(3, { _ -> Point2D.ZERO })
            positions[0] = position(richGraph!!.getEdgeSource(richEdge).element)
            positions[1] = position(richEdge.subNode)
            positions[2] = position(richGraph!!.getEdgeTarget(richEdge).element)

            positionMap[richEdge.edge] = positions
        }

        return positionMap
    }

    private fun initializeSetGraphs() {
        val vN = nodes.size

        // Minimum guaranteed distance graph.
        minDistGraph = SimpleWeightedGraph(DefaultWeightedEdge::class.java)
        for (v in network.graph.vertexSet()) {
            minDistGraph!!.addVertex(v)
        }
        for (e in network.graph.edgeSet()) {
            val s = network.graph.getEdgeSource(e)
            val sI = index!![s]
            val t = network.graph.getEdgeTarget(e)
            val tI = index!![t]
            val nE = minDistGraph!!.addEdge(s, t)
            minDistGraph!!.setEdgeWeight(nE, EDGE_SPACE + mD!![sI!!][tI!!])
        }

        // Construct shortest path distance matrix on original graph,
        // for distance graph and node overlap constraints.
        val paths = FloydWarshallShortestPaths(minDistGraph!!)
        D = Array(vN) { DoubleArray(vN) }
        for (i in 0 until vN)
            for (j in i + 1 until vN) {
                D!![j][i] = paths.shortestDistance(nodes[i], nodes[j])
                D!![i][j] = D!![j][i]
            }

        // Spanning graph per set.
        spanGraphs = ArrayList()
        for (set in sets) {
            val weightedSubGraph = SimpleWeightedGraph<NetworkNode, DefaultEdge>(DefaultWeightedEdge::class.java)
            for (v in set.nodes) {
                weightedSubGraph.addVertex(v)
            }
            val coreEdges = HashSet<DefaultEdge>()
            val nodes = ArrayList(set.nodes)
            for (i in nodes.indices) {
                val s = nodes[i]

                for (j in i + 1 until set.nodes.size) {
                    val t = nodes[j]
                    val nE = weightedSubGraph.addEdge(s, t)

                    // Guarantee MST along already present edges.
                    val isCore = network.graph.containsEdge(s, t)
                    weightedSubGraph.setEdgeWeight(nE, if (isCore) 0.0 else D!![index!![s!!]!!][index!![t!!]!!])
                    if (isCore) {
                        coreEdges.add(nE)
                    }
                }
            }

            // Combine spanning and core edges into set spanning graph.
            val spanGraph = SimpleGraph<NetworkNode, DefaultEdge>(DefaultEdge::class.java)
            for (v in set.nodes) {
                spanGraph.addVertex(v)
            }
            for (e in coreEdges) {
                spanGraph.addEdge(weightedSubGraph.getEdgeSource(e), weightedSubGraph.getEdgeTarget(e))
            }

            if (!weightedSubGraph.edgeSet().isEmpty()) {
                val spanningEdges = PrimMinimumSpanningTree(weightedSubGraph).minimumSpanningTreeEdgeSet
                for (e in spanningEdges) {
                    spanGraph.addEdge(weightedSubGraph.getEdgeSource(e), weightedSubGraph.getEdgeTarget(e))
                }
            }

            spanGraphs!!.add(spanGraph)
        }

        // Construct rich graph (containing all membership information).
        richGraph = SimpleWeightedGraph(RichEdge::class.java)
        richIndex = HashMap()

        // Base nodes.
        for (i in nodes.indices) {
            val n = nodes[i]
            val rN = RichNode(n)
            rN.memberships.addAll(nodeMemberships[n]!!)
            richGraph!!.addVertex(rN)
        }
        // Add all core edges.
        for (e in network.graph.edgeSet()) {
            val rSN = RichNode(network.graph.getEdgeSource(e))
            val rTN = RichNode(network.graph.getEdgeTarget(e))
            val rE = richGraph!!.addEdge(rSN, rTN)
            rE.edge = e
            rE.core = true
            richGraph!!.setEdgeWeight(rE, D!![index!![rSN.element]!!][index!![rTN.element]!!])
        }
        // Add all set span edges.
        for (i in sets.indices) {
            val sG = spanGraphs!![i]

            for (e in sG.edgeSet()) {
                val rSN = RichNode(sG.getEdgeSource(e))
                val rTN = RichNode(sG.getEdgeTarget(e))
                var rE: RichEdge? = richGraph!!.addEdge(rSN, rTN)

                if (rE == null) {
                    rE = richGraph!!.getEdge(rSN, rTN)
                } else {
                    rE.core = false
                    val rSI = index!![rSN.element]
                    val rTI = index!![rTN.element]
                    richGraph!!.setEdgeWeight(rE, Math.max(mD!![rSI!!][rTI!!],
                            SET_EDGE_CONTRACTION / D!![rSI][rTI]))
                }
            }
        }
        // Infer edge to set memberships from matching vertices.
        for (e in richGraph!!.edgeSet()) {
            val rSN = richGraph!!.getEdgeSource(e)
            val rTN = richGraph!!.getEdgeTarget(e)
            e.memberships.addAll(this.nodeMemberships[rSN.element]!!)
            e.memberships.retainAll(this.nodeMemberships[rTN.element]!!)
        }

        // Construct rich graph that has been extended by one dummy node per edge.
        richNodes = arrayOfNulls(vN + richGraph!!.edgeSet().size)
        extRichGraph = SimpleWeightedGraph(RichEdge::class.java)
        // Base nodes.
        for (i in nodes.indices) {
            val n = nodes[i]
            val rN = RichNode(n)
            richNodes!![i] = rN
            richIndex!![rN] = i
            extRichGraph!!.addVertex(rN)
        }
        // Add edges, but include additional dummy node.
        for ((j, e) in richGraph!!.edgeSet().withIndex()) {
            val rSN = richGraph!!.getEdgeSource(e)
            val rTN = richGraph!!.getEdgeTarget(e)

            val dN = RichNode(null)
            extRichGraph!!.addVertex(dN)
            e.subNode = dN
            richNodes!![nodes.size + j] = dN
            richIndex!![dN] = nodes.size + j

            val sE = extRichGraph!!.addEdge(rSN, dN)
            sE.core = e.core
            val tE = extRichGraph!!.addEdge(dN, rTN)
            tE.core = e.core

            val hW = 0.5 * richGraph!!.getEdgeWeight(e)
            extRichGraph!!.setEdgeWeight(sE, hW)
            extRichGraph!!.setEdgeWeight(tE, hW)

        }
    }

    // Set membership discrepancy between two nodes.
    private fun membershipDiscrepancy(n1: NetworkNode, n2: NetworkNode): Int {
        var discr = 0

        val sets1 = nodeMemberships[n1]
        val sets2 = nodeMemberships[n2]
        for (s in sets1!!)
            if (!s.nodes.contains(n2))
                discr++
        for (s in sets2!!)
            if (!s.nodes.contains(n1))
                discr++

        return discr
    }

    private inner class BoundProjection(private val radii: DoubleArray, private val distances: Array<DoubleArray>) {
        private val xVariables: Array<Variable?> = arrayOfNulls(radii.size)
        private val yVariables: Array<Variable?> = arrayOfNulls(radii.size)

        init {

            for (i in radii.indices) {
                xVariables[i] = Variable(0.0, 1.0, 1.0)
                yVariables[i] = Variable(0.0, 1.0, 1.0)
            }
        }

        fun projectFunctions(): Array<Descent.Projection> {
            return arrayOf(object : Descent.Projection() {
                override fun apply(x0: DoubleArray, y0: DoubleArray, r: DoubleArray) {
                    xProject(x0, y0, r)
                }
            }, object : Descent.Projection() {
                override fun apply(x0: DoubleArray, y0: DoubleArray, r: DoubleArray) {
                    yProject(x0, y0, r)
                }
            })
        }

        private fun xProject(x0: DoubleArray, y0: DoubleArray, x: DoubleArray) {
            solve(xVariables, createConstraints(x0, y0, true), x0, x)
        }

        private fun yProject(x0: DoubleArray, y0: DoubleArray, y: DoubleArray) {
            solve(yVariables, createConstraints(x0, y0, false), y0, y)
        }

        private fun createConstraints(x0: DoubleArray, y0: DoubleArray, xAxis: Boolean): Array<Constraint> {
            val cs = ArrayList<Constraint>()

            // Pair wise constraints, only when within distance bounds.
            // Limit to plain nodes, for now.
            for (i in nodes.indices) {
                val iP = Point2D(x0[i], y0[i])

                for (j in nodes.indices) {
                    val jP = Point2D(x0[j], y0[j])

                    val ijDD = this.distances[i][j]  // Desired distance.
                    if (ijDD > Math.abs(y0[i] - y0[j]) || // Rough distance cut optimization.
                            ijDD > Math.abs(x0[i] - x0[j])) {
                        val iR = this.radii[i]
                        val jR = this.radii[j]

                        val xM = Point2D(0.5 * (iP.x + jP.x + if (iP.x < jP.x)
                            this.radii[i] - this.radii[j]
                        else
                            this.radii[j] - this.radii[i]),
                                0.5 * (iP.y + jP.y)) // Point between two vertex lines.
                        val iM = Point2D(Math.min(iP.x + iR, Math.max(iP.x - iR, xM.x)), iP.y)
                        val jM = Point2D(Math.min(jP.x + jR, Math.max(jP.x - jR, xM.x)), jP.y)
                        val ijV = jM.subtract(iM)  // Minimum distance vector between vertex lines.
                        val ijAD = ijV.magnitude()      // Actual distance between vertex lines.

                        // Create constraint when distance is violated.
                        if (ijDD > ijAD) {
                            var lV: Variable
                            var rV: Variable
                            var gap: Double

                            // Use ij vector angle to determine axis of constraint.
                            if (xAxis && iM.x != jM.x) {
                                lV = if (iP.x < jP.x) xVariables[i]!! else xVariables[j]!!
                                rV = if (iP.x < jP.x) xVariables[j]!! else xVariables[i]!!
                                gap = this.radii[i] + this.radii[j] + ijV.normalize().multiply(ijDD).x

                                cs.add(Constraint(lV, rV, gap, false))
                            }

                            if (!xAxis /*&& Math.abs(ijV[0]) < Math.abs(ijV[1])*/) {
                                lV = if (iP.y < jP.y) this.yVariables[i]!! else this.yVariables[j]!!
                                rV = if (iP.y < jP.y) this.yVariables[j]!! else this.yVariables[i]!!
                                gap = ijV.normalize().multiply(ijDD).y

                                cs.add(Constraint(lV, rV, gap, false))
                            }
                        }
                    }
                }
            }

            return cs.toTypedArray()
        }

        private fun solve(vs: Array<Variable?>,
                          cs: Array<Constraint>,
                          starting: DoubleArray,
                          desired: DoubleArray) {
            val solver = Solver(vs, cs)
            solver.setStartingPositions(starting)
            solver.setDesiredPositions(desired)
            solver.solve()

            // Push solution as result.
            for (i in vs.indices) {
                desired[i] = vs[i]!!.position()
            }
        }
    }

    class RichNode(var element: NetworkNode?) {
        var memberships: MutableList<NetworkAnnotation> = ArrayList()

        override fun hashCode(): Int {
            return if (element == null) super.hashCode() else this.element!!.hashCode()
        }

        override fun equals(obj: Any?): Boolean {
            val other = obj as RichNode?
            return if (element == null) super.equals(obj) else element == other!!.element
        }
    }

    class RichEdge : DefaultWeightedEdge() {
        var edge: NetworkLink? = null
        var core: Boolean = false            // Whether edge is part of original graph.
        var memberships: MutableList<NetworkAnnotation> = ArrayList()  // Set memberships.
        var subNode: RichNode? = null        // Optional dummy node that divides edge in extended graph.

    }

    companion object {

        const val RIBBON_WIDTH = 8.0
        const val RIBBON_SPACE = 2.0
        const val RIBBON_EXTENT = RIBBON_WIDTH + RIBBON_SPACE
        const val LINK_WIDTH = 3.0
        const val NODE_OUTLINE = 8.0
        const val NODE_SPACE = 2.0
        const val NODE_MARGIN = 0.5 * NODE_OUTLINE + NODE_SPACE
        const val BUFFER_SEGMENTS = 5
        const val LINK_SEGMENTS = 10

        private const val EDGE_SPACE = 50.0
        private const val INITIAL_ITERATIONS = 10000
        private const val PHASE_ITERATIONS = 10000
        private const val SET_EDGE_CONTRACTION = 0.5

        // Dimensions of drawn node label.
        fun labelDimensions(label: String, padding: Boolean): Point2D {
            val text = Text(label)
            // TODO: Switch to actual network node dimensions.
            text.style = "-fx-font-family: Source Sans Pro Regular; -fx-font-size: 1.2em; " +
                    if (padding) "-fx-padding: 0 .5em 0 .5em"
                    else ""
            text.applyCss()

            val bounds = text.boundsInLocal
            return Point2D(bounds.width,bounds.height + NODE_OUTLINE)
        }

        fun labelSpacedDimensions(label: String): Point2D {
            return labelDimensions(label, true).add(NODE_OUTLINE + NODE_SPACE, NODE_OUTLINE + NODE_SPACE)
        }
    }
}
