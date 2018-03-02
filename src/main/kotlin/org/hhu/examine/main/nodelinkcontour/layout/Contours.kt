package org.hhu.examine.main.nodelinkcontour.layout

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion
import org.hhu.examine.data.NetworkAnnotation
import org.hhu.examine.main.nodelinkcontour.layout.Paths.GEOMETRY_FACTORY
import java.util.*
import java.util.Collections.emptyList

/**
 * Generates annotation contours from a network layout.
 */
class Contours {

    val annotation: NetworkAnnotation
    val ribbon: Geometry
    val outline: Geometry

    constructor(annotation: NetworkAnnotation) {
        this.annotation = annotation
        this.ribbon = GEOMETRY_FACTORY.buildGeometry(emptyList<Any>())
        this.outline = GEOMETRY_FACTORY.buildGeometry(emptyList<Any>())
    }

    constructor(annotation: NetworkAnnotation, layout: Layout) {
        this.annotation = annotation

        // Radius for smoothening contours.
        val smoothRadius = 4 * Layout.RIBBON_EXTENT

        val vertexHulls = ArrayList<Geometry>()
        for (v in annotation.nodes) {
            // Radius of set around vertex.
            val vertexIndex = 1.01 + layout.nodeMemberships[v]!!.indexOf(annotation)
            val edgeRadius = vertexIndex * Layout.RIBBON_EXTENT + smoothRadius

            // Radius of vertex (assuming rounded rectangle).
            val vertexBounds = Layout.labelDimensions(v, false)
            val vertexPos = layout.position(v)
            val vertexRadius = 0.5 * vertexBounds.y + Layout.NODE_MARGIN
            val totalRadius = vertexRadius + edgeRadius

            val line = GEOMETRY_FACTORY.createLineString(
                    arrayOf(Coordinate(vertexPos.x - 0.5 * vertexBounds.x, vertexPos.y), Coordinate(vertexPos.x + 0.5 * vertexBounds.x, vertexPos.y)))
            val hull = line.buffer(totalRadius, Layout.BUFFER_SEGMENTS)

            vertexHulls.add(hull)
        }

        val linkHulls = ArrayList<Geometry>()
        for (e in layout.richGraph!!.edgeSet()) {
            val ind = e.memberships.indexOf(annotation)

            if (ind >= 0) {
                val sN = layout.richGraph!!.getEdgeSource(e)
                val sP = layout.position(sN.element)
                val tN = layout.richGraph!!.getEdgeTarget(e)
                val tP = layout.position(tN.element)
                val dN = e.subNode
                val dP = layout.position(dN)
                val hasCore = layout.network.graph.containsEdge(sN.element, tN.element)

                // Radius of set around vertex.
                val edgeIndex = 0.51 + ind
                val edgeRadius = edgeIndex * Layout.RIBBON_EXTENT + smoothRadius +
                        if (hasCore) Layout.LINK_WIDTH + Layout.RIBBON_SPACE else 0.0  // Widen for contained edge.

                val line = Paths.circlePiece(sP, dP, tP, Layout.LINK_SEGMENTS)
                val hull = line.buffer(edgeRadius, Layout.BUFFER_SEGMENTS)

                linkHulls.add(hull)
            }
        }

        // Vertex anti-membership hulls.
        val antiVertices = HashSet(layout.network.graph.vertexSet())
        antiVertices.removeAll(annotation.nodes)
        val vertexAntiHulls = ArrayList<Geometry>()
        for (v in antiVertices) {
            // Radius of vertex (assuming rounded rectangle).
            val bounds = Layout.labelDimensions(v, false)
            val pos = layout.position(v)
            val radius = 0.5 * bounds.y + Layout.NODE_OUTLINE

            val line = GEOMETRY_FACTORY.createLineString(
                    arrayOf(Coordinate(pos.x - 0.5 * bounds.x, pos.y), Coordinate(pos.x + 0.5 * bounds.x, pos.y)))
            val hull = line.buffer(radius, Layout.BUFFER_SEGMENTS)

            vertexAntiHulls.add(hull)
        }

        val vertexContour = convexHulls(Paths.fastUnion(vertexHulls))
        val linkContour = Paths.fastUnion(linkHulls)
        val fullContour = vertexContour.union(linkContour)
        var smoothenedContour = fullContour.buffer(-smoothRadius, Layout.BUFFER_SEGMENTS)

        if (!vertexAntiHulls.isEmpty()) {
            val antiContour = CascadedPolygonUnion(vertexAntiHulls).union()
            smoothenedContour = smoothenedContour.difference(antiContour)
            // Safeguard link contours, TODO: fix anti-hull vs link cases.
            smoothenedContour = smoothenedContour.union(
                    linkContour.buffer(-smoothRadius, Layout.BUFFER_SEGMENTS))
        }

        val innerContour = smoothenedContour.buffer(-Layout.RIBBON_WIDTH, Layout.BUFFER_SEGMENTS)
        val ribbon = smoothenedContour.difference(innerContour)

        this.ribbon = ribbon
        this.outline = smoothenedContour
    }

    private fun convexHulls(g: Geometry): Geometry {
        val gN = g.numGeometries

        val sG = ArrayList<Geometry>()
        for (i in 0 until gN) {
            sG.add(g.getGeometryN(i).convexHull())
        }

        return CascadedPolygonUnion(sG).union()
    }

}
