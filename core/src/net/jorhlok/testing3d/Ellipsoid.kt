package net.jorhlok.testing3d

import com.badlogic.gdx.math.Vector3

//some code adapted from http://www.peroxide.dk/papers/collision/collision.pdf

class Ellipsoid() {
    val eRadius = Vector3()

    //regular 3D space
    val R3Velocity = Vector3()
    val R3Position = Vector3()

    //ellipsoid to unit sphere space or R3/eRadius
    val velocity = Vector3()
    val normalizedVelocity = Vector3()
    val basePoint = Vector3()

    var foundCollision = false
    var nearestDistance = 0f
    val intersectionPoint = Vector3()

    //get lowest positive root
    fun getLowestRoot(a: Float, b: Float, c: Float, maxR: Float): Float {
        if (a == 0f) return -1f //dodge melting the cpu, or worse, with a divide by zero

        val determinant = b*b - 4f*a*c

        if (determinant < 0) return -1f

        if (determinant == 0f) {
            val r1 = -b / (2*a)
            if (r1 > 0 && r1 < maxR) return r1
            else return -1f
        }

        val sqrtD = Math.sqrt(determinant.toDouble()).toFloat()
        var r1 = (-b - sqrtD) / (2*a)
        var r2 = (-b + sqrtD) / (2*a)

        if (r1 > r2) r1 = r2.also { r2 = r1 } //swap

        if (r1 > 0 && r1 < maxR) return r1

        if (r2 > 0 && r2 < maxR) return r2

        return -1f
    }

    //triangle should already be in ellipsoid space
    fun checkTriangle(tri: Tri) {
        var t0 = 0f
        var t1 = 0f
        var embeddedInPlane = false

        val distPlane = tri.distanceTo(basePoint)
        val normalDotVelocity = tri.normal.dot(velocity)

        if (normalDotVelocity == 0f) {
            if (Math.abs(distPlane) >= 1f) return //no collision possible
            embeddedInPlane = true
            t1 = 1f
        }
        else {
            t0 = (-1-distPlane)/normalDotVelocity
            t1 = (1-distPlane)/normalDotVelocity
            if (t0 > t1) t0 = t1.also { t1 = t0 } //swap
            if (t0 > 1 || t1 < 0) return //no collision possible
            t0 = t0.coerceIn(0f..1f)
            t1 = t1.coerceIn(0f..1f)
        }

        var hasCollided = false
        val collisionPoint = Vector3()
        var t = 1f

        if (!embeddedInPlane) {
            collisionPoint.set(basePoint.cpy().sub(tri.normal).add(velocity.cpy().scl(t0)))
            if (tri.checkPointInside(collisionPoint)) {
                hasCollided = true
                t = t0
            }
        }

        if (!hasCollided) {
            val velocitySqrLen = velocity.len2()

            //check points
            var a = velocitySqrLen
            var b = 2 * velocity.dot(basePoint.cpy().sub(tri.p0))
            var c = tri.p0.cpy().sub(basePoint).len2() - 1
            var tn = getLowestRoot(a,b,c,t) //using t as maxR limits to sooner collisions and any of the ones below could be it
            if (tn >= 0) {
                t = tn
                hasCollided = true
                collisionPoint.set(tri.p0)
            }

            b = 2 * velocity.dot(basePoint.cpy().sub(tri.p1))
            c = tri.p1.cpy().sub(basePoint).len2() - 1
            tn = getLowestRoot(a,b,c,t)
            if (tn >= 0) {
                t = tn
                hasCollided = true
                collisionPoint.set(tri.p1)
            }

            b = 2 * velocity.dot(basePoint.cpy().sub(tri.p2))
            c = tri.p2.cpy().sub(basePoint).len2() - 1
            tn = getLowestRoot(a,b,c,t)
            if (tn >= 0) {
                t = tn
                hasCollided = true
                collisionPoint.set(tri.p2)
            }

            //check edges
            var edge = tri.p1.cpy().sub(tri.p0)
            var baseToVertex = tri.p0.cpy().sub(basePoint)
            var edgeSqrLen = edge.len2()
            var edgeDotVelocity = edge.dot(velocity)
            var edgeDotBaseToVertex = edge.dot(baseToVertex)

            a = edgeSqrLen*-velocitySqrLen + edgeDotVelocity*edgeDotVelocity
            b = edgeSqrLen*2*velocity.dot(baseToVertex) - 2*edgeDotVelocity*edgeDotBaseToVertex
            c = edgeSqrLen*(1-baseToVertex.len2()) + edgeDotBaseToVertex*edgeDotBaseToVertex

            tn = getLowestRoot(a,b,c,t)
            if(tn >= 0) {
                val f = (edgeDotVelocity*tn-edgeDotBaseToVertex)/edgeSqrLen
                if (f in 0f..1f) {
                    t = tn
                    hasCollided = true
                    collisionPoint.set(tri.p0.cpy().add(edge.scl(f)))
                }
            }

            edge = tri.p2.cpy().sub(tri.p1)
            baseToVertex = tri.p1.cpy().sub(basePoint)
            edgeSqrLen = edge.len2()
            edgeDotVelocity = edge.dot(velocity)
            edgeDotBaseToVertex = edge.dot(baseToVertex)

            a = edgeSqrLen*-velocitySqrLen + edgeDotVelocity*edgeDotVelocity
            b = edgeSqrLen*2*velocity.dot(baseToVertex) - 2*edgeDotVelocity*edgeDotBaseToVertex
            c = edgeSqrLen*(1-baseToVertex.len2()) + edgeDotBaseToVertex*edgeDotBaseToVertex

            tn = getLowestRoot(a,b,c,t)
            if(tn >= 0) {
                val f = (edgeDotVelocity*tn-edgeDotBaseToVertex)/edgeSqrLen
                if (f in 0f..1f) {
                    t = tn
                    hasCollided = true
                    collisionPoint.set(tri.p1.cpy().add(edge.scl(f)))
                }
            }

            edge = tri.p0.cpy().sub(tri.p2)
            baseToVertex = tri.p0.cpy().sub(basePoint)
            edgeSqrLen = edge.len2()
            edgeDotVelocity = edge.dot(velocity)
            edgeDotBaseToVertex = edge.dot(baseToVertex)

            a = edgeSqrLen*-velocitySqrLen + edgeDotVelocity*edgeDotVelocity
            b = edgeSqrLen*2*velocity.dot(baseToVertex) - 2*edgeDotVelocity*edgeDotBaseToVertex
            c = edgeSqrLen*(1-baseToVertex.len2()) + edgeDotBaseToVertex*edgeDotBaseToVertex

            tn = getLowestRoot(a,b,c,t)
            if(tn >= 0) {
                val f = (edgeDotVelocity*tn-edgeDotBaseToVertex)/edgeSqrLen
                if (f in 0f..1f) {
                    t = tn
                    hasCollided = true
                    collisionPoint.set(tri.p2.cpy().add(edge.scl(f)))
                }
            }
        }

        if (hasCollided) {
            val distToCollision = t*velocity.len()
            if (!foundCollision || distToCollision < nearestDistance) {
                nearestDistance = distToCollision
                intersectionPoint.set(collisionPoint)
                foundCollision = true
            }
        }
    }

    fun checkTriangleFront(tri: Tri) {
        if (tri.isFrontFacingTo(normalizedVelocity)) checkTriangle(tri)
    }

    fun checkTriangleBack(tri: Tri) {
        if (!tri.isFrontFacingTo(normalizedVelocity)) checkTriangle(tri)
    }
    
}