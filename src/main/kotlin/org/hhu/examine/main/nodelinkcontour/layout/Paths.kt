package org.hhu.examine.main.nodelinkcontour.layout

import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.geom.Polygon
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion
import javafx.geometry.Point2D
import javafx.scene.shape.*
import java.lang.Math.*
import java.util.*
import java.util.Collections.emptyList

/**
 * Geometry utility functions.
 */
object Paths {

    val GEOMETRY_FACTORY = GeometryFactory()  // JTS geometry factory.

    // Convert a JTS geometry to a Java shape.
    fun geometryToShape(geometry: Geometry?): List<PathElement> {
        return if (geometry == null) emptyList() else geometryToShape(geometry, 0.0)
    }

    fun geometryToShape(geometry: Geometry, arcFactor: Double): List<PathElement> {

        val pathElements = ArrayList<PathElement>()
        geometryToShape(geometry, pathElements, arcFactor)
        return pathElements
    }

    private fun geometryToShape(
            geometry: Geometry,
            pathElements: MutableList<PathElement>,
            arcFactor: Double) {

        if (geometry.numGeometries > 1) {
            for (i in 0 until geometry.numGeometries) {
                geometryToShape(geometry.getGeometryN(i), pathElements, arcFactor)
            }
        } else if (geometry is Polygon) {
            polygonToShape(geometry, pathElements, arcFactor)
        }
    }

    // Attach JTS polygon to a shape.
    private fun polygonToShape(
            polygon: Polygon,
            path: MutableList<PathElement>,
            arcFactor: Double) {

        // Exterior ring.
        ringToShape(polygon.exteriorRing, path, arcFactor)

        // Interior rings.
        for (i in 0 until polygon.numInteriorRing) {
            ringToShape(polygon.getInteriorRingN(i), path, arcFactor)
        }
    }

    // Attach JTS ring to a shape.
    private fun ringToShape(
            string: LineString,
            pathElements: MutableList<PathElement>,
            arcDegrees: Double) {

        val cs = string.coordinates

        // Derive smooth arcs from sampled JTS arcs.
        if (arcDegrees > 0) {
            val vs = arrayOfNulls<Point2D>(cs.size)
            for (i in cs.indices) {
                vs[i] = Point2D(cs[cs.size - i - 1].x, cs[cs.size - i - 1].y)
            }

            // Similar region break points.
            val breakPoints = ArrayList<Int>()
            for (i in 0 until vs.size - 1) {

                val l = vs[i]
                val mI = (i + 1) % (vs.size - 1)
                val m1 = vs[mI]
                val m2 = vs[(i + 2) % (vs.size - 1)]
                val r = vs[(i + 3) % (vs.size - 1)]

                val lm = m1!!.subtract(l)
                val m = m2!!.subtract(m1)
                val mr = r!!.subtract(m2)

                if (Math.abs(deltaAngle(lm, m) - deltaAngle(m, mr)) > arcDegrees) {
                    breakPoints.add(mI)
                }
            }

            // Construct path from similar regions.
            for (i in breakPoints.indices) {

                // Breakpoint to ... + 1 is a straight line.
                val firstBreak = breakPoints[i]
                val begin = vs[firstBreak % (vs.size - 1)]

                // Breakpoint to next breakpoint is arc, iff applicable.
                val nextBreak = breakPoints[(i + 1) % breakPoints.size] % (vs.size - 1)
                var bD: Int
                bD = 0
                while ((firstBreak + bD) % (vs.size - 1) != nextBreak) {
                    bD++
                }
                bD /= 2
                val midC = (firstBreak + bD) % (vs.size - 1)
                val mid = vs[midC]
                val end = vs[nextBreak]

                pathElements.addAll(getArc(begin!!, mid!!, end!!))
            }

            pathElements.add(ClosePath())
        } else {
            pathElements.add(MoveTo(cs[0].x, cs[0].y))
            for (j in 1 until cs.size) {
                pathElements.add(LineTo(cs[j].x, cs[j].y))
            }
            pathElements.add(ClosePath())
        }// Path according to JTS samples.
    }

    // Shorthand for a fast and safe JTS union.
    fun fastUnion(gs: List<Geometry>): Geometry {
        return if (gs.isEmpty())
            GEOMETRY_FACTORY.createGeometryCollection(arrayOf())
        else
            CascadedPolygonUnion(gs).union()
    }

    fun circlePiece(
            p1: Point2D,
            p2: Point2D,
            p3: Point2D,
            segments: Int): LineString {

        val v21 = p2.subtract(p1)
        val d21 = v21.dotProduct(v21)
        val v31 = p3.subtract(p1)
        val d31 = v31.dotProduct(v31)
        val a4 = 2 * v21.crossProduct(v31).z

        val d13 = p1.distance(p3)
        val wellFormed = p1.distance(p2) < d13 && p2.distance(p3) < d13

        val lS: LineString
        if (false && wellFormed && Math.abs(a4) > 0.001) {
            val center = Point2D(
                    p1.x + (v31.y * d21 - v21.y * d31) / a4,
                    p1.y + (v21.x * d31 - v31.x * d21) / a4
            )
            val radius = Math.sqrt(
                    d21 * d31 *
                            (Math.pow(p3.x - p2.x, 2.0) + Math.pow(p3.y - p2.y, 2.0))
            ) / Math.abs(a4)

            var a1 = deltaAngle(center, p1)
            val a2 = deltaAngle(center, p2)
            var a3 = deltaAngle(center, p3)
            if (a2 < a1 && a2 < a3 || a2 > a1 && a2 > a3) {
                if (a1 < a3) {
                    a3 -= 2 * PI
                } else {
                    a1 -= 2 * PI
                }
            }

            val cs = arrayOfNulls<Coordinate>(segments)
            for (i in 0 until segments) {
                val fI = i.toDouble() / (segments - 1).toDouble()
                val aI = (1 - fI) * a1 + fI * a3
                val vI = unitCirclePoint(aI).multiply(radius).add(center)
                cs[i] = Coordinate(vI.x, vI.y)
            }

            lS = GEOMETRY_FACTORY.createLineString(cs)
        } else {
            val cs = arrayOfNulls<Coordinate>(2)
            cs[0] = Coordinate(p1.x, p1.y)
            cs[1] = Coordinate(p3.x, p3.y)
            lS = GEOMETRY_FACTORY.createLineString(cs)
        }// There is no circle, so take a straight line between p0 and p1.

        return lS
    }

    /**
     * Draw an arc through the three given points.
     *
     * @param p1 The start point of the arc.
     * @param p2 The point that the arc passes through.
     * @param p3 The end point of the arc.
     * @return An arc that passes through the three given points.
     */
    fun getArc(p1: Point2D, p2: Point2D, p3: Point2D): List<PathElement> {

        val path = ArrayList<PathElement>()
        path.add(MoveTo(p1.x, p1.y))

        val v21 = p2.subtract(p1)
        val d21 = v21.dotProduct(v21)
        val v31 = p3.subtract(p1)
        val d31 = v31.dotProduct(v31)
        val d13 = p1.distance(p3)

        val wellFormed = p1.distance(p2) < d13 && p2.distance(p3) < d13
        if (wellFormed) {
            val a4 = 2 * v21.crossProduct(v31).z
            val radius = Math.sqrt(d21 * d31 *
                    (Math.pow(p3.x - p2.x, 2.0) + Math.pow(p3.y - p2.y, 2.0))) / Math.abs(a4)
            val cross = p2.subtract(p1).crossProduct(p3.subtract(p2)).z > 0

            path.add(ArcTo(radius, radius, 0.0, p3.x, p3.y, false, cross))
        } else {
            path.add(LineTo(p3.x, p3.y))
        }// There is no circle, so take a straight line between p0 and p1.

        return path
    }

    /**
     * Compute the angle, in radians, of the difference from origin to target.
     *
     * @param origin The point of origin.
     * @param target The target point; which lies at returned angle with respect to origin.
     * @return The angle from origin to target.
     */
    private fun deltaAngle(origin: Point2D, target: Point2D): Double {
        val delta = target.subtract(origin)
        return Math.atan2(delta.x, delta.y)
    }

    /**
     * The point that lies on the unit circle at the given counter-clockwise angle with
     * respect to the origin and (0,1).
     *
     * @param angle The counter-clockwise angle between the returned point, the origin, and (0,1).
     * @return The point that lies on the unit circle.
     */
    private fun unitCirclePoint(angle: Double): Point2D {
        return Point2D(cos(angle), sin(angle))
    }

}
