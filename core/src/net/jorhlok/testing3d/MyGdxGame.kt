package net.jorhlok.testing3d

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.utils.Array


class MyGdxGame : ApplicationAdapter() {
    var environment: Environment? = null
    var cam: PerspectiveCamera? = null
    var camController: CameraInputController? = null
    var modelBatch: ModelBatch? = null
    var assets: AssetManager? = null
    var instances = Array<ModelInstance>()
    var loading: Boolean = false
    var data = ""

    var terrain = TriSoup()

    override fun create() {
        modelBatch = ModelBatch()
        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

//        DefaultShader.defaultCullFace = 0

        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam!!.position.set(7f, 7f, 7f)
        cam!!.lookAt(0f, 0f, 0f)
        cam!!.near = 1/64f
        cam!!.far = 1024f
        cam!!.update()

        camController = CameraInputController(cam)
        Gdx.input.inputProcessor = camController

        assets = AssetManager()
        assets!!.load(data + "sampleterraintri.g3dj", Model::class.java)
        assets!!.load(data + "ellipsoid.g3dj", Model::class.java)
        loading = true

        terrain.FromJSONFile("sampleterraintri.g3dj")
    }

    override fun render() {
        if (loading && assets!!.update())
            doneLoading()
        camController!!.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch!!.begin(cam)
        modelBatch!!.render(instances, environment)
        modelBatch!!.end()
    }

    override fun dispose() {
        modelBatch?.dispose()
        instances.clear()
        assets?.dispose()
    }

    private fun doneLoading() {
//        val ship = assets!!.get(data + "sampleterraintri.g3db", Model::class.java)
//        var x = -5f
//        while (x <= 5f) {
//            var z = -5f
//            while (z <= 5f) {
//                val shipInstance = ModelInstance(ship)
//                shipInstance.transform.setToTranslation(x, 0f, z)
//                instances.add(shipInstance)
//                z += 2f
//            }
//            x += 2f
//        }
        val terrain = assets!!.get(data + "sampleterraintri.g3dj", Model::class.java)
        val ellipsoid = assets!!.get(data + "ellipsoid.g3dj", Model::class.java)
        instances.add(ModelInstance(terrain))
        instances.add(ModelInstance(ellipsoid))
        loading = false
    }
}
