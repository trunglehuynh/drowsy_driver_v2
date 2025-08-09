package com.galaxylab.drowsydriver.AI


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Rect
import com.google.firebase.ml.vision.face.FirebaseVisionFace

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic(
    overlay: GraphicOverlay,
    private val firebaseVisionFace: FirebaseVisionFace?,
    private val isLandScapeMode: Boolean,
    private val overlayBitmap: Bitmap?
) :
    GraphicOverlay.Graphic(overlay) {

    /**
     * Draws the face annotations for position on the supplied canvas.
     */

    private val facePositionPaint = Paint().apply {
        color = Color.WHITE
    }

    private val idPaint = Paint().apply {
        color = Color.WHITE
        textSize = ID_TEXT_SIZE
    }

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(45f)
//        paint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
//        paint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
//        paint.setPathEffect(new CornerPathEffect(10) );   // set the path effect when they join.

//        this.rou
    }

    override fun draw(canvas: Canvas?) {
        val face = firebaseVisionFace ?: return
        if (canvas == null) return
        val boundingBox = face.boundingBox
        if (isLandScapeMode) {
            drawBox(
                canvas,
                boundingBox.centerX().toFloat(),
                boundingBox.centerY().toFloat(),
                scaleY(boundingBox.height() / 2.0f),
                scaleX(boundingBox.width() / 2.0f)
            )
        } else {
            drawBox(
                canvas,
                boundingBox.centerX().toFloat(),
                boundingBox.centerY().toFloat(),
                scaleX(boundingBox.width() / 2.0f),
                scaleY(boundingBox.height() / 2.0f)
            )
        }

//        canvas.drawText("ID: " + face.trackingId, x + ID_X_OFFSET, y - 3 * ID_Y_OFFSET, idPaint)


        /*
           // Draws a circle at the position of the detected face, with the face's track id below.
        // An offset is used on the Y axis in order to draw the circle, face id and happiness level in the top area
        // of the face's bounding box

        NOT remove
        canvas.drawCircle(x, y - 4 * ID_Y_OFFSET, FACE_POSITION_RADIUS, facePositionPaint)
        canvas.drawText("id: " + face.trackingId, x + ID_X_OFFSET, y - 3 * ID_Y_OFFSET, idPaint)
        canvas.drawText(
            "happiness: ${String.format("%.2f", face.smilingProbability)}",
            x + ID_X_OFFSET * 3,
            y - 2 * ID_Y_OFFSET,
            idPaint
        )
        if (facing == CameraSource.CAMERA_FACING_FRONT) {
            canvas?.drawText(
                "right eye: ${String.format("%.2f", face.rightEyeOpenProbability)}",
                x - ID_X_OFFSET,
                y,
                idPaint
            )
            canvas?.drawText(
                "left eye: ${String.format("%.2f", face.leftEyeOpenProbability)}",
                x + ID_X_OFFSET * 6,
                y,
                idPaint
            )
        } else {
            canvas?.drawText(
                "left eye: ${String.format("%.2f", face.leftEyeOpenProbability)}",
                x - ID_X_OFFSET,
                y,
                idPaint
            )
            canvas?.drawText(
                "right eye: ${String.format("%.2f", face.rightEyeOpenProbability)}",
                x + ID_X_OFFSET * 6,
                y,
                idPaint
            )

        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.MOUTH_BOTTOM)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.LEFT_CHEEK)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.LEFT_EAR)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.MOUTH_LEFT)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.LEFT_EYE)
        drawBitmapOverLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.NOSE_BASE)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.RIGHT_CHEEK)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.RIGHT_EAR)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.RIGHT_EYE)
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.MOUTH_RIGHT)
        */
    }

    private fun drawBox(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        xOffset: Float,
        yOffset: Float
    ) {
        val x = translateX(centerX)
        val y = translateY(centerY)

        // Draws a bounding box around the face.
//        var xOffset = scaleX(width / 2.0f)
//        var yOffset = scaleY(height / 2.0f)

        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas.drawRect(left, top, right, bottom, boxPaint)
    }

    private fun drawLandmarkPosition(canvas: Canvas, face: FirebaseVisionFace, landmarkID: Int) {
        val landmark = face.getLandmark(landmarkID)
        landmark?.let {
            val point = it.position
            canvas.drawCircle(
                translateX(point.x),
                translateY(point.y),
                10f, idPaint
            )
        }
    }

    private fun drawBitmapOverLandmarkPosition(
        canvas: Canvas,
        face: FirebaseVisionFace,
        landmarkID: Int
    ) {
        val landmark = face.getLandmark(landmarkID) ?: return

        val point = landmark.position

        overlayBitmap?.let {
            val imageEdgeSizeBasedOnFaceSize = face.boundingBox.width() / 4.0f

            val left = (translateX(point.x) - imageEdgeSizeBasedOnFaceSize).toInt()
            val top = (translateY(point.y) - imageEdgeSizeBasedOnFaceSize).toInt()
            val right = (translateX(point.x) + imageEdgeSizeBasedOnFaceSize).toInt()
            val bottom = (translateY(point.y) + imageEdgeSizeBasedOnFaceSize).toInt()

            canvas.drawBitmap(
                it, null,
                Rect(left, top, right, bottom), null
            )
        }
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 4.0f
        private const val ID_TEXT_SIZE = 50.0f
        private const val ID_Y_OFFSET = 50.0f
        private const val ID_X_OFFSET = -50.0f
        private const val BOX_STROKE_WIDTH = 25.0f
    }
}
