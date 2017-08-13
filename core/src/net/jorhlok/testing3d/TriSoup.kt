package net.jorhlok.testing3d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.JsonReader



class TriSoup() {
    val soup = Array<Tri>()

    constructor(uri: String) : this() {
        FromJSONFile(uri)
    }

    fun FromJSONFile(uri: String) {
        FromJSON(Gdx.files.internal(uri).readString())
    }

    fun FromJSON(text: String) {
        val root = JsonReader().parse(text)
        val meshes = root["meshes"]
        if (meshes.isArray){
            for (i in 0..meshes.size-1){
                val vertices = meshes[i]["vertices"].asFloatArray()
                val points = Array<Vector3>()
                for (j in 0..(vertices.size-1)/3) {
                    points.add(Vector3(vertices[j*3],vertices[j*3+1],vertices[j*3+2]))
                }
                val parts = meshes[i]["parts"]
                if (parts.isArray) {
                    for (j in 0..parts.size-1){
                        val indices = parts[j]["indices"].asIntArray()
                        for (k in 0..(indices.size-1)/3) {
                            soup.add(Tri(points[indices[k*3]],points[indices[k*3+1]],points[indices[k*3+2]]))
                        }
                    }
                }
            }
        }
    }
}