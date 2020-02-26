package com.google.ar.sceneform.samples.hellosceneform

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class HelloSceneformActivity : AppCompatActivity() {
    private val TAG = HelloSceneformActivity::class.java.simpleName
    private val MIN_OPENGL_VERSION = 3.0

    private var arFragment: ArFragment? = null
    private var andyRenderable: ModelRenderable? = null
    private var testViewRenderable: ViewRenderable? = null
    private var redSphereRenderable: ModelRenderable? = null
    private var yodaRenderable: ModelRenderable? = null

    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_ux)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept { renderable: ModelRenderable? -> andyRenderable = renderable }
                .exceptionally { throwable: Throwable? ->
                    val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }
        // Demo view renderable
        ViewRenderable.builder()
                .setView(this, R.layout.test_view)
                .build()
                .thenAccept { renderable: ViewRenderable? -> testViewRenderable = renderable }
        // Make a shape from a factory
        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.RED))
                .thenAccept { material: Material? -> redSphereRenderable = ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.15f, 0.0f), material) }
        ModelRenderable.builder()
                .setSource(this, Uri.parse("scene.sfb"))
                .build()
                .thenAccept { renderable: ModelRenderable? -> yodaRenderable = renderable }
        val spotlight = Light.builder(Light.Type.FOCUSED_SPOTLIGHT)
                .setColor(Color(android.graphics.Color.YELLOW))
                .setShadowCastingEnabled(true)
                .build()
        arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (yodaRenderable == null) {
                return@setOnTapArPlaneListener
            }
            // Create the Anchor.
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment!!.arSceneView.scene)
            anchorNode.light = spotlight
            // Create the transformable andy and add it to the anchor.
            val andy = TransformableNode(arFragment!!.transformationSystem)
            andy.setParent(anchorNode)
            andy.renderable = yodaRenderable
            andy.light = spotlight
            andy.select()
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     *
     * Finishes the activity if Sceneform can not run
     */
    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show()
            activity.finish()
            return false
        }
        return true
    }
}