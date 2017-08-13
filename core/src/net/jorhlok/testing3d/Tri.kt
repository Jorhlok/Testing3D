package net.jorhlok.testing3d

import com.badlogic.gdx.math.Vector3

//some code adapted from http://www.peroxide.dk/papers/collision/collision.pdf

class Tri(val p0: Vector3, val p1: Vector3, val p2: Vector3) {
    val normal = Vector3()
    var eq3 = 0f
    init{
        normal.set(p1.cpy().sub(p0).crs(p2.cpy().sub(p0)).nor())
        eq3 = -(normal.x*p0.x + normal.y*p0.y + normal.z*p0.z)
    }

    fun isFrontFacingTo(dir: Vector3) = normal.dot(dir) <= 0

    fun distanceTo(pt: Vector3) = pt.dot(normal) + eq3

    fun checkPointInside(pt: Vector3): Boolean {
        val e10 = p1.cpy().sub(p0)
        val e20 = p2.cpy().sub(p0)

        val a = e10.dot(e10)
        val b = e10.dot(e20)
        val c = e20.dot(e20)
        val ac_bb = a*c-b*b
        val vp = pt.cpy().sub(p0)

        val d = vp.dot(e10)
        val e = vp.dot(e20)
        val x = (d*c)-(e*b)
        val y = (e*a)-(d*b)
        val z = x+y-ac_bb

        //C++ code is:
        //(uint32(z) & ~(uint32(x) | uint32(y))) & 0x80000000
//        System.out.println("$x, $y, $z")
        //the below two lines return differently
//        return 0 != (z.toInt().and(x.toInt().or(y.toInt()))*-1-1).and(Int.MIN_VALUE)
        return z<0 && (x>=0 || y>=0) //I think this is what it means, maybe

    }
}