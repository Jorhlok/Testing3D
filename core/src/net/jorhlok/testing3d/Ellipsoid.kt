package net.jorhlok.testing3d

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array

//some code adapted from http://www.peroxide.dk/papers/collision/collision.pdf
//then I switched to https://github.com/jrouwe/SweptEllipsoid

class Ellipsoid() {
    val eRadius = Vector3()

    //regular 3D space
    val Velocity = Vector3()
    val Position = Vector3()

    //ellipsoid to unit sphere space or R3/eRadius
//    val velocity = Vector3()
//    val normalizedVelocity = Vector3()
//    val basePoint = Vector3()
//
    var foundCollision = false
    var nearestDistance = 0f
    val intersectionPoint = Vector3()
    val translation = Vector3()

    //get lowest positive root
    fun getLowestRoot(a: Float, b: Float, c: Float, maxR: Float): Float {
        if (a == 0f) return -1f //dodge melting the cpu, or worse, with a divide by zero

        val determinant = b*b - 4f*a*c

        if (determinant < 0) return -1f

        if (determinant == 0f) {
            val r1 = -b / (2*a)
            if (r1 > 0 && r1 < maxR) return r1
            return -1f
        }

        val sqrtD = Math.sqrt(determinant.toDouble()).toFloat()
        var r1 = (-b - sqrtD) / (2*a)
        var r2 = (-b + sqrtD) / (2*a)

        if (r1 > r2) r1 = r2.also { r2 = r1 } //swap

        if (r1 > 0 && r1 < maxR) return r1

        if (r2 > 0 && r2 < maxR) return r2

        return -1f
    }


    fun PlaneSweptSphereIntersect(plane: Plane, inBegin: Vector3, inDelta: Vector3, inRadius: Float, outT: Vector2): Boolean {
        val ndotd = plane.normal.dot(inDelta)
        val disttob = plane.getDistance(inBegin)
        if (ndotd == 0f) {
            if (Math.abs(disttob) > inRadius) return false
            outT.set(0f,1f)
        } else {
            outT.x = (inRadius-disttob)/ndotd
            outT.y = (-inRadius-disttob)/ndotd

            if (outT.x > outT.y) outT.x = outT.y.also { outT.y = outT.x }

            if (outT.x > 1 || outT.y < 0) return false

            outT.x.coerceAtLeast(0f)
            outT.y.coerceAtMost(1f)
        }
        return true
    }

    fun PolygonContains(v: Array<Vector2>, inPt: Vector2): Boolean {

        for (i in 0 until v.size) {
            val v1 = v[i]
            val v2 = v[(i+1)%v.size]

            val v1v2 = v2.cpy().sub(v1)
            val v1pt = inPt.cpy().sub(v1)
            if (v1v2.x * v1pt.y - v1pt.x * v1v2.y > 0) return false
        }
        return true
    }

    fun PolygonCircleIntersect(v: Array<Vector2>, inPt: Vector2, radiusSq: Float, outPt: Vector2): Boolean {
        if (PolygonContains(v,inPt)) {
            outPt.set(inPt)
            return true
        }

        var collision = false
        var inRadiusSq = radiusSq

        for (i in 0 until v.size) {
            val v1 = v[i]
            val v2 = v[(i+1)%v.size]
            val v1v2 = v2.cpy().sub(v1)
            val v1pt = inPt.cpy().sub(v1)
            val fraction = v1pt.dot(v1v2)
            if (fraction > 0) {
                val distsq = v1pt.len2()
                if (distsq <= inRadiusSq) {
                    collision = true
                    outPt.set(v1)
                    inRadiusSq = distsq
                }
            } else {
                val v1v2len2 = v1v2.len2()
                if (fraction <= v1v2len2) {
                    val pt = v1.cpy().add(v1v2.scl(fraction / v1v2len2))
                    val distsq = pt.cpy().sub(inPt).len2()
                    if (distsq <= inRadiusSq) {
                        collision = true
                        outPt.set(pt)
                        inRadiusSq = distsq
                    }
                }
            }
        }



        return collision
    }

    //outPt x and y represent a Vector2 point, outPt z represents the time from 0f to 1f or "outFraction" I think
    fun SweptCircleEdgeVertexIntersect(v: Array<Vector2>, inPt: Vector2, inDelta: Vector2, a: Float, b: Float, c: Float, outPt: Vector3) : Boolean {
        var upperbound = 1f
        var collision = false
        var t = 0f


        for (i in 0 until v.size) {
            val v1 = v[i]
            val v2 = v[(i + 1) % v.size]
            val bv1 = v1.cpy().sub(inPt)
            val a1 = a - inDelta.len2()
            val b1 = b + 2 * inDelta.dot(bv1)
            val c1 = c - bv1.len2()
            t = getLowestRoot(a1, b1, c1, upperbound)
            if (t >= 0) {
                collision = true
                upperbound = t
                outPt.x = v1.x
                outPt.y = v1.y
            }
            val v1v2 = v2.cpy().sub(v1)
            val v1v2dotdelta = v1v2.dot(inDelta)
            val v1v2dotbv1 = v1v2.dot(bv1)
            val v1v2len2 = v1v2.len2()
            val a2 = v1v2len2 * a1 + v1v2dotdelta * v1v2dotdelta
            val b2 = v1v2len2 * b1 - 2 * v1v2dotbv1 * v1v2dotdelta
            val c2 = v1v2len2 * c1 + v1v2dotbv1 * v1v2dotbv1
            t = getLowestRoot(a2, b2, c2, upperbound)
            if (t >= 0) {
                val f = t * v1v2dotdelta - v1v2dotbv1
                if (f in 0f..v1v2len2) {
                    collision = true
                    upperbound = t
                    v1v2.scl(f / v1v2len2).add(v1)
                    outPt.x = v1v2.x
                    outPt.y = v1v2.y
                }
            }
        }


        outPt.z = upperbound
        return collision
    }

    //pretendo that outFraction is a float instead
    fun PolygonSweptSphereIntersect(plane: Plane, v: Array<Vector2>, inPt: Vector3, inDelta: Vector3, inRadius: Float, outPt: Vector3, outFraction: Vector2): Boolean {
        val t = Vector2()
        if (!PlaneSweptSphereIntersect(plane,inPt,inDelta,inRadius,t)) return false

        val ndotd = plane.normal.dot(inDelta)
        val distb = plane.getDistance(inPt)
        val a = -ndotd*ndotd
        val b = -2*ndotd*distb
        val c = inRadius*inRadius - distb*distb

        val begin = plane.worldToPlane(inPt)
        val delta = plane.worldToPlane(inDelta)

        val p = Vector2()
        if (PolygonCircleIntersect(v,delta.cpy().scl(t.x).add(begin),a*t.x*t.x+b*t.x+c,p)) {
            outFraction.x = t.x
            outPt.set(plane.planeToWorld(p))
            return true
        }

        val pandf = Vector3()
        if (SweptCircleEdgeVertexIntersect(v,begin,delta,a,b,c,pandf)) {
            outFraction.x = pandf.z
            outPt.set(plane.planeToWorld(p.set(pandf.x,pandf.y)))
            return true
        }

        return false
    }

    fun Transform2x2(mat: Matrix4, vec: Vector2) = Vector2(mat.values[0]*vec.x+mat.values[1]*vec.y, mat.values[4]*vec.x+mat.values[5]*vec.y)

    //pretendo that outFraction is a float instead
    fun PolygonSweptEllipsoidIntersect(plane: Plane, v: Array<Vector2>, usstoworld: Matrix4, usstoworldtra: Matrix4, worldtouss: Matrix4, beginuss: Vector3, deltauss: Vector3, outPt: Vector3, outFraction: Vector2): Boolean {
        val planeuss = plane.getTransformedByInverseTra(usstoworldtra)

        val t = Vector2()
        if (!PlaneSweptSphereIntersect(planeuss,beginuss,deltauss,1f,t)) return false

        val planetoworld = plane.PlaneToWorldMatrix()
        val planeusstouss = planeuss.PlaneToWorldMatrix()
        val planetoplaneuss = plane.Invert4x3(planeusstouss).mul(worldtouss).mul(planetoworld)

        val ndotd = planeuss.normal.dot(deltauss)
        val distb = planeuss.getDistance(beginuss)
        val a = -ndotd * ndotd
        val b = -2 * ndotd * distb
        val c = 1 - distb * distb

        planeuss.U.set(planeusstouss.values[0],planeusstouss.values[1],planeusstouss.values[2])
        planeuss.V.set(planeusstouss.values[4],planeusstouss.values[5],planeusstouss.values[6])

        val trans = Vector2(planeusstouss.values[3],planeusstouss.values[7])
        val begin = planeuss.worldToPlane(beginuss).sub(trans)
        val delta = planeuss.worldToPlane(deltauss)

        val vuss = Array<Vector2>()

        for (i in 0 until v.size)
            vuss.add(Transform2x2(planetoplaneuss,v[i]))

        val p = Vector2()
        if(PolygonCircleIntersect(vuss,delta.cpy().scl(t.x).add(begin),a*t.x*t.x+b*t.x+c,p)) {
            outFraction.x = t.x
            outPt.set(p.x+trans.x,p.y+trans.y,0f).mul(planeusstouss).mul(usstoworld)
            return true
        }

        val pandf = Vector3()
        if (SweptCircleEdgeVertexIntersect(vuss,begin,delta,a,b,c,pandf)) {
            outFraction.x = pandf.z
            outPt.set(pandf.x,pandf.y,0f).mul(planeusstouss).mul(usstoworld)
            return true
        }

        return false
    }

    fun TriSoupSweptEllipsoidIntersect(triSoup: TriSoup, deltatime: Float): Boolean {
        val usstoworld = Matrix4().setToScaling(eRadius)
        val usstoworldtra = usstoworld.cpy().tra()
        val worldtouss = usstoworld.cpy().inv()

        val beginuss = Position.cpy().mul(worldtouss)
        val deltauss = Velocity.cpy().scl(deltatime).mul(worldtouss)

        val outPt = Vector3()
        val outFraction = Vector2()

        for (tri in triSoup.soup) {
            if (PolygonSweptEllipsoidIntersect(tri.plane,tri.vertecies,usstoworld,usstoworldtra,worldtouss,beginuss,deltauss,outPt,outFraction)) {
                if (!foundCollision || nearestDistance > outFraction.x) {
                    foundCollision = true
                    nearestDistance = outFraction.x
                    intersectionPoint.set(outPt)
                }
            }
        }

        if (foundCollision) translation.set(Velocity).scl(deltatime*nearestDistance)

        return foundCollision
    }


    //triangle should already be in ellipsoid space
//    fun checkTriangle(tri: Tri) {
//        var t0 = 0f
//        var t1 = 0f
//        var embeddedInPlane = false
//
//        val distPlane = tri.distanceTo(basePoint)
//        val normalDotVelocity = tri.normal.dot(velocity)
//
//        if (normalDotVelocity == 0f) {
//            if (Math.abs(distPlane) >= 1f) return //no collision possible
//            embeddedInPlane = true
//            t1 = 1f
//        }
//        else {
//            t0 = (-1-distPlane)/normalDotVelocity
//            t1 = (1-distPlane)/normalDotVelocity
//            if (t0 > t1) t0 = t1.also { t1 = t0 } //swap
//            if (t0 > 1 || t1 < 0) return //no collision possible
//            t0 = t0.coerceIn(0f..1f)
//            t1 = t1.coerceIn(0f..1f)
//        }
//
//        var hasCollided = false
//        val collisionPoint = Vector3()
//        var t = 1f
//
//        if (!embeddedInPlane) {
//            collisionPoint.set(basePoint.cpy().sub(tri.normal).add(velocity.cpy().scl(t0)))
//            if (tri.checkPointInside(collisionPoint)) {
//                hasCollided = true
//                t = t0
//            }
//        }
//
//        if (!hasCollided) {
//            val velocitySqrLen = velocity.len2()
//
//            //check points
//            var a = velocitySqrLen
//            var b = 2 * velocity.dot(basePoint.cpy().sub(tri.p0))
//            var c = tri.p0.cpy().sub(basePoint).len2() - 1
//            var tn = getLowestRoot(a,b,c,t) //using t as maxR limits to sooner collisions and any of the ones below could be it
//            if (tn >= 0) {
//                t = tn
//                hasCollided = true
//                collisionPoint.set(tri.p0)
//            }
//
//            b = 2 * velocity.dot(basePoint.cpy().sub(tri.p1))
//            c = tri.p1.cpy().sub(basePoint).len2() - 1
//            tn = getLowestRoot(a,b,c,t)
//            if (tn >= 0) {
//                t = tn
//                hasCollided = true
//                collisionPoint.set(tri.p1)
//            }
//
//            b = 2 * velocity.dot(basePoint.cpy().sub(tri.p2))
//            c = tri.p2.cpy().sub(basePoint).len2() - 1
//            tn = getLowestRoot(a,b,c,t)
//            if (tn >= 0) {
//                t = tn
//                hasCollided = true
//                collisionPoint.set(tri.p2)
//            }
//
//            //check edges
//            var edge = tri.p1.cpy().sub(tri.p0)
//            var baseToVertex = tri.p0.cpy().sub(basePoint)
//            var edgeSqrLen = edge.len2()
//            var edgeDotVelocity = edge.dot(velocity)
//            var edgeDotBaseToVertex = edge.dot(baseToVertex)
//
//            a = edgeSqrLen*-velocitySqrLen + edgeDotVelocity*edgeDotVelocity
//            b = edgeSqrLen*2*velocity.dot(baseToVertex) - 2*edgeDotVelocity*edgeDotBaseToVertex
//            c = edgeSqrLen*(1-baseToVertex.len2()) + edgeDotBaseToVertex*edgeDotBaseToVertex
//
//            tn = getLowestRoot(a,b,c,t)
//            if(tn >= 0) {
//                val f = (edgeDotVelocity*tn-edgeDotBaseToVertex)/edgeSqrLen
//                if (f in 0f..1f) {
//                    t = tn
//                    hasCollided = true
//                    collisionPoint.set(tri.p0.cpy().add(edge.scl(f)))
//                }
//            }
//
//            edge = tri.p2.cpy().sub(tri.p1)
//            baseToVertex = tri.p1.cpy().sub(basePoint)
//            edgeSqrLen = edge.len2()
//            edgeDotVelocity = edge.dot(velocity)
//            edgeDotBaseToVertex = edge.dot(baseToVertex)
//
//            a = edgeSqrLen*-velocitySqrLen + edgeDotVelocity*edgeDotVelocity
//            b = edgeSqrLen*2*velocity.dot(baseToVertex) - 2*edgeDotVelocity*edgeDotBaseToVertex
//            c = edgeSqrLen*(1-baseToVertex.len2()) + edgeDotBaseToVertex*edgeDotBaseToVertex
//
//            tn = getLowestRoot(a,b,c,t)
//            if(tn >= 0) {
//                val f = (edgeDotVelocity*tn-edgeDotBaseToVertex)/edgeSqrLen
//                if (f in 0f..1f) {
//                    t = tn
//                    hasCollided = true
//                    collisionPoint.set(tri.p1.cpy().add(edge.scl(f)))
//                }
//            }
//
//            edge = tri.p0.cpy().sub(tri.p2)
//            baseToVertex = tri.p2.cpy().sub(basePoint)
//            edgeSqrLen = edge.len2()
//            edgeDotVelocity = edge.dot(velocity)
//            edgeDotBaseToVertex = edge.dot(baseToVertex)
//
//            a = edgeSqrLen*-velocitySqrLen + edgeDotVelocity*edgeDotVelocity
//            b = edgeSqrLen*2*velocity.dot(baseToVertex) - 2*edgeDotVelocity*edgeDotBaseToVertex
//            c = edgeSqrLen*(1-baseToVertex.len2()) + edgeDotBaseToVertex*edgeDotBaseToVertex
//
//            tn = getLowestRoot(a,b,c,t)
//            if(tn >= 0) {
//                val f = (edgeDotVelocity*tn-edgeDotBaseToVertex)/edgeSqrLen
//                if (f in 0f..1f) {
//                    t = tn
//                    hasCollided = true
//                    collisionPoint.set(tri.p2.cpy().add(edge.scl(f)))
//                }
//            }
//        }
//
//        if (hasCollided) {
//            val distToCollision = t*velocity.len()
//            if (!foundCollision || distToCollision < nearestDistance) {
//                nearestDistance = distToCollision
//                intersectionPoint.set(collisionPoint)
//                foundCollision = true
//            }
//        }
//    }
//
//    fun checkTriangleFront(tri: Tri) {
//        if (tri.isFrontFacingTo(normalizedVelocity)) checkTriangle(tri)
//    }
//
//    fun checkTriangleBack(tri: Tri) {
//        if (!tri.isFrontFacingTo(normalizedVelocity)) checkTriangle(tri)
//    }
//
//    fun R3toE3() {
//        basePoint.set(Position).scl(inverseERadius())
//        velocity.set(Velocity).scl(inverseERadius())
//        normalizedVelocity.set(velocity).nor()
//    }
//
//    fun inverseERadius() = Vector3(1/eRadius.x,1/eRadius.y,1/eRadius.z)
}