package com.galaxylab.drowsydriver.AI

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector


class GraphicOverlay(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    //    lateinit var configuation:CameraConfiguration
//    private fun setCameraConfiguration(configuation:CameraConfiguration){
//        this.configuation = configuation
//    }
    private var facing: Int = CameraSelector.LENS_FACING_BACK

    private val graphics: MutableList<Graphic> = ArrayList()

    /* access modifiers changed from: private */
    var heightScaleFactor = 1.0f
    private val lock = Any()
    private var previewHeight = 0
    private var previewWidth = 0

    /* access modifiers changed from: private */
    var widthScaleFactor = 1.0f

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas?)
        fun scaleX(horizontal: Float): Float {
            return overlay.widthScaleFactor * horizontal
        }

        fun scaleY(vertical: Float): Float {
            return overlay.heightScaleFactor * vertical
        }

        val applicationContext: Context
            get() = overlay.context.applicationContext

        fun translateX(x: Float): Float {
            return if (overlay.facing == 0) {
//                Timber.d("overlay.getWidth() ${overlay.width}")
                overlay.width.toFloat() - scaleX(x)
            } else scaleX(x)
        }

        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            overlay.postInvalidate()
        }

    }

    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) { graphics.add(graphic) }
    }

    fun remove(graphic: Graphic?) {
        synchronized(lock) { graphics.remove(graphic) }
        postInvalidate()
    }

    fun setCameraInfo(previewWidth2: Int, previewHeight2: Int, facing2: Int) {
        previewWidth = previewWidth2
        previewHeight = previewHeight2
        facing = facing2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            if (!(previewWidth == 0 || previewHeight == 0)) {
                widthScaleFactor =
                    width.toFloat() / previewWidth.toFloat()
                heightScaleFactor =
                    height.toFloat() / previewHeight.toFloat()
            }
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}