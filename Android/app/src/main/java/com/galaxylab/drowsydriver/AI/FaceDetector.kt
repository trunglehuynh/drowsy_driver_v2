package com.galaxylab.drowsydriver.AI


import android.graphics.Bitmap
import android.media.Image
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions


class FaceDetector {

    private val detector: FirebaseVisionFaceDetector

    init {
        val build: FirebaseVisionFaceDetectorOptions =
            FirebaseVisionFaceDetectorOptions.Builder()
                .setMinFaceSize(0.5f)
                .enableTracking()
//                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)

                .build()

        detector = FirebaseVision.getInstance().getVisionFaceDetector(build)
    }

    private fun detectFacesInImage(image: FirebaseVisionImage?): Task<MutableList<FirebaseVisionFace>> {
        val detectInImage = detector.detectInImage(image!!)
        return detectInImage
    }

    fun detectFace(mediaImage: Image, rotationDegree: Int): Task<DetectedFaces> {
        val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(
            mediaImage,
            degreesToFirebaseRotation(rotationDegree)
        )
        val taskCompletionSource =
            com.google.android.gms.tasks.TaskCompletionSource<DetectedFaces>()
        detectFacesInImage(firebaseVisionImage)
            .addOnSuccessListener { faces ->
                val largestFace = faces
                    .filter {
                        it.boundingBox.width() >= CameraConfiguration.minBoundingBox &&
                                it.boundingBox.height() >= CameraConfiguration.minBoundingBox
                    }
                    .maxByOrNull {
                        it.boundingBox.height().coerceAtMost(it.boundingBox.width())
                    }

                taskCompletionSource.setResult(
                    DetectedFaces(
                        firebaseVisionImage.bitmap,
                        largestFace
                    )
                )
            }
            .addOnFailureListener { e ->
                taskCompletionSource.setException(e)
            }

        return taskCompletionSource.task
    }

    private fun degreesToFirebaseRotation(degrees: Int): Int {
        return when (degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }
    }
}

data class DetectedFaces(val bitmap: Bitmap, val face: FirebaseVisionFace?)