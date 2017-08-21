package net.jorhlok.testing3d

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class Plane {
    val normal = Vector3()
    var constant = 0f

    //basis vectors?
    val U = Vector3()
    val V = Vector3()

    fun calcUV() {
        U.set(getPerpendicular(normal))
        V.set(normal.cpy().crs(U))
    }

    fun getPerpendicular(inVec: Vector3): Vector3 {
        val ret = Vector3()
        if(Math.abs(inVec.x) > Math.abs(inVec.y)) {
            val len = Math.sqrt((inVec.x * inVec.x + inVec.z * inVec.z).toDouble()).toFloat()
            ret.set(inVec.z/len,0f,-inVec.x/len)
        } else {
            val len = Math.sqrt((inVec.y * inVec.y + inVec.z * inVec.z).toDouble()).toFloat()
            ret.set(0f,inVec.z/len,-inVec.y/len)
        }
        return ret
    }

    fun getDistance(inPoint: Vector3) = inPoint.cpy().dot(normal) + constant

    fun worldToPlane(inPoint: Vector3) = Vector2(U.dot(inPoint), V.dot(inPoint))

    fun planeToWorld(inPoint: Vector2) = U.cpy().scl(inPoint.x).add(V.cpy().scl(inPoint.y)).sub(normal.cpy().scl(constant))

    fun PlaneToWorldMatrix(): Matrix4 {
        val ret = Matrix4()
        ret.values[0] = U.x
        ret.values[1] = U.y
        ret.values[2] = U.z
//        ret.values[3] = 0f
        ret.values[4] = V.x
        ret.values[5] = V.y
        ret.values[6] = V.z
//        ret.values[7] = 0f
        ret.values[8] = normal.x
        ret.values[9] = normal.y
        ret.values[10] = normal.z
//        ret.values[11] = 0f
        ret.values[12] = normal.x * -constant
        ret.values[13] = normal.y * -constant
        ret.values[14] = normal.z * -constant
//        ret.values[15] = 1f
        return ret
    }

    fun Invert4x3(mat: Matrix4): Matrix4 {
        val m = Matrix4()
        var det = mat.values[Matrix4.M00] * (mat.values[Matrix4.M11] * mat.values[Matrix4.M22] - mat.values[Matrix4.M21] * mat.values[Matrix4.M12])
        det          -= mat.values[Matrix4.M10] * (mat.values[Matrix4.M01] * mat.values[Matrix4.M22] - mat.values[Matrix4.M21] * mat.values[Matrix4.M02])
        det          += mat.values[Matrix4.M20] * (mat.values[Matrix4.M01] * mat.values[Matrix4.M12] - mat.values[Matrix4.M11] * mat.values[Matrix4.M02])
        if (det == 0f) throw RuntimeException("non-invertible matrix")
        det = 1/det

        m.values[Matrix4.M00] = (mat.values[Matrix4.M11] * mat.values[Matrix4.M22] - mat.values[Matrix4.M21] * mat.values[Matrix4.M12]) * det
        m.values[Matrix4.M10] = (mat.values[Matrix4.M10] * mat.values[Matrix4.M22] - mat.values[Matrix4.M12] * mat.values[Matrix4.M20]) * -det
        m.values[Matrix4.M20] = (mat.values[Matrix4.M10] * mat.values[Matrix4.M21] - mat.values[Matrix4.M11] * mat.values[Matrix4.M20]) * det

        m.values[Matrix4.M01] = (mat.values[Matrix4.M01] * mat.values[Matrix4.M22] - mat.values[Matrix4.M21] * mat.values[Matrix4.M02]) * -det
        m.values[Matrix4.M11] = (mat.values[Matrix4.M00] * mat.values[Matrix4.M22] - mat.values[Matrix4.M20] * mat.values[Matrix4.M02]) * det
        m.values[Matrix4.M21] = (mat.values[Matrix4.M00] * mat.values[Matrix4.M21] - mat.values[Matrix4.M20] * mat.values[Matrix4.M01]) * -det

        m.values[Matrix4.M02] = (mat.values[Matrix4.M01] * mat.values[Matrix4.M12] - mat.values[Matrix4.M11] * mat.values[Matrix4.M02]) * det
        m.values[Matrix4.M12] = (mat.values[Matrix4.M00] * mat.values[Matrix4.M12] - mat.values[Matrix4.M10] * mat.values[Matrix4.M02]) * -det
        m.values[Matrix4.M22] = (mat.values[Matrix4.M00] * mat.values[Matrix4.M11] - mat.values[Matrix4.M10] * mat.values[Matrix4.M01]) * det

        m.values[Matrix4.M30] = -(mat.values[Matrix4.M30] * m.values[Matrix4.M00] + mat.values[Matrix4.M31] * m.values[Matrix4.M10] - mat.values[Matrix4.M32] * m.values[Matrix4.M20])
        m.values[Matrix4.M31] = -(mat.values[Matrix4.M30] * m.values[Matrix4.M01] + mat.values[Matrix4.M31] * m.values[Matrix4.M11] - mat.values[Matrix4.M32] * m.values[Matrix4.M21])
        m.values[Matrix4.M32] = -(mat.values[Matrix4.M30] * m.values[Matrix4.M02] + mat.values[Matrix4.M31] * m.values[Matrix4.M12] - mat.values[Matrix4.M32] * m.values[Matrix4.M22])

        return m
    }

    fun getTransformedByInverseTra(mat: Matrix4): Plane {
        val plane = Plane()

        plane.constant = constant + normal.dot(0f,0f,mat.scaleZ)
        plane.normal.set(normal).mul(mat)

        val reciplen = 1/plane.normal.len()
        plane.normal.scl(reciplen)
        plane.constant *= reciplen
//        plane.calcUV()

        return plane
    }
}