package com.galaxylab.drowsydriver.UI

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.galaxylab.drowsydriver.AI.DetectedFaces
import com.galaxylab.drowsydriver.AI.FaceDetector
import com.galaxylab.drowsydriver.AlertController
import com.galaxylab.drowsydriver.UserInfo
import com.galaxylab.drowsydriver.UserInfo.Companion.DEFAULT_DURATION_THRESHOLD_ALERT
import com.galaxylab.drowsydriver.UserInfo.Companion.DEFAULT_EMPTY_FACE_ALERT
import com.galaxylab.drowsydriver.UserInfo.Companion.DEFAULT_SENSITIVE_THRESHOLD_ALERT
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import timber.log.Timber


class CameraFragmentVM(
    private val faceDetector: FaceDetector,
    private val alertController: AlertController,
    private val userInfo: UserInfo,
) : ViewModel(), ImageAnalysis.Analyzer {
    private val TAG = "CameraFragmentVM"

    private val EMPTY_FACE_ALERT_GAP = 5000L // 3 seconds
    private val EMPTY_FACE_ALERT_GAP_IN_PIP_MODE = 10000L // 3 seconds
    private val MAX_FRAME_RATE: Long = 10L
    private val GAP_BETWEEN_FRAME: Long = 1000 / MAX_FRAME_RATE
    private var lastUpdate = 0L
    private var sensitiveThresholdAlert = userInfo.getSensitiveThreshold()
    private var durationThresholdAlert = userInfo.getDurationThreshold()
    private var isAlertEmptyFace = userInfo.isAlertEmptyFace()
    private val detectedFaces = MutableLiveData<DetectedFaces>()
    private var isInPictureInPictureMode = false
    var currentRotationDegreeIndex = 0
    var emptyFaceDetectionFrameInPIPMode = 3
    val rotationDegrees = listOf(0, 90, 180, 270)

    fun getDetectedFaces(): LiveData<DetectedFaces> = detectedFaces
    private val closedEyeAlertEvent = MutableLiveData(false)
    fun getClosedEyeAlertEvent(): LiveData<Boolean> = closedEyeAlertEvent
    private val emptyFaceAlertEvent = MutableLiveData(false)
    fun getEmptyFaceAlertEvent(): LiveData<Boolean> = emptyFaceAlertEvent


    fun getSensitiveThresholdAlert(): Int = sensitiveThresholdAlert
    fun getDurationThresholdAlert(): Long = durationThresholdAlert
    fun getIsAlertEmptyFace(): Boolean = isAlertEmptyFace

    fun setSensitiveThresholdAlert(threshold: Int) {
        sensitiveThresholdAlert = threshold
        userInfo.updateSensitiveThreshold(threshold)
    }

    fun setDurationThresholdAlert(threshold: Long) {
        durationThresholdAlert = threshold
        userInfo.updateDurationThreshold(threshold)
    }

    fun setAlertEmptyFace(isAlert: Boolean) {
        isAlertEmptyFace = isAlert
        userInfo.updateAlertEmptyFace(isAlert)
    }

    fun setPiPMode(isInPictureInPictureMode: Boolean) {
        this.isInPictureInPictureMode = isInPictureInPictureMode
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!shouldUpdate()) {
            imageProxy.close()
            return
        }
        if (isInPictureInPictureMode) {
            detectImageInPipMode(imageProxy)
        } else {
            detectImageInNonPipMode(imageProxy)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun detectImageInNonPipMode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        faceDetector.detectFace(mediaImage, imageProxy.imageInfo.rotationDegrees)
            .addOnSuccessListener { result ->
                detectedFaces.postValue(result)
                maySendAlert(result)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Face detection failed")
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun detectImageInPipMode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        Timber.d("detectImageInPipMode ${rotationDegrees[currentRotationDegreeIndex]}")
        faceDetector.detectFace(mediaImage, rotationDegrees[currentRotationDegreeIndex])
            .addOnSuccessListener { result ->
                if (result.face == null) {
                    --emptyFaceDetectionFrameInPIPMode
                    Timber.d("detectImageInPipMode no face")
                } else {
                    emptyFaceDetectionFrameInPIPMode = 3
                }
                if (emptyFaceDetectionFrameInPIPMode == 0) {
                    currentRotationDegreeIndex =
                        (currentRotationDegreeIndex + 1) % rotationDegrees.size
                    emptyFaceDetectionFrameInPIPMode = 3
                }

                detectedFaces.postValue(result)
                maySendAlert(result)
                imageProxy.close()

            }
            .addOnFailureListener { e ->
                Timber.e(e, "Face detection failed")
                imageProxy.close()
            }
    }

    private fun shouldUpdate(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate >= GAP_BETWEEN_FRAME) {
            lastUpdate = currentTime
            return true
        }
        return false
    }

    private var lastTimeCloseEye: Long? = null
    private var lastTimeDetectFace: Long? = null
//    private val ONE_SECONDS = 3000L
//    private val HAFT_SECONDS = 500L

    private fun maySendAlert(detectedFaces: DetectedFaces) {
//        Timber.d("$TAG, detection sensitive $isSensitiveDetection")
        //does not see any faces
        if (detectedFaces.face == null) {
            // should not reset lastTimeCloseEye because some time the app will miss some frames
//            lastTimeCloseEye = null

            if (!isAlertEmptyFace) {
                return
            }
            val gap =
                if (isInPictureInPictureMode) EMPTY_FACE_ALERT_GAP_IN_PIP_MODE else EMPTY_FACE_ALERT_GAP
            val shouldAlertEmptyFace =
                lastTimeDetectFace != null && (System.currentTimeMillis() - lastTimeDetectFace!!) > gap
            if (!shouldAlertEmptyFace) {
                return
            }
            emptyFaceAlertEvent.postValue(true)
            alertController.alert()
            return
        }
        emptyFaceAlertEvent.postValue(false)

        lastTimeDetectFace = System.currentTimeMillis()
        val isEyeClose = isEyeClosed(detectedFaces.face)

        if (!isEyeClose) {
            lastTimeCloseEye = null
            closedEyeAlertEvent.postValue(false)
            return
        }

        val current = System.currentTimeMillis()
        if (lastTimeCloseEye == null) {
            lastTimeCloseEye = current
            closedEyeAlertEvent.postValue(false)
            Timber.d("first time close")
            return
        }
//        Log.d("durationThresholdAlert", durationThresholdAlert.toString())

        val gap = current - lastTimeCloseEye!!
        val shouldAlert = gap >= durationThresholdAlert //|| isSensitiveDetection
        maySendAlert(shouldAlert)
        Timber.d("shouldAlert $shouldAlert")
    }

    private fun maySendAlert(shouldAlert: Boolean) {
        closedEyeAlertEvent.postValue(shouldAlert)
        if (shouldAlert) alertController.alert()
    }

    private fun isEyeClosed(face: FirebaseVisionFace): Boolean {
        val normalizeThreshold = sensitiveThresholdAlert.toFloat() / 100
//        Timber.d("leftEyeOpenProbability ${face.leftEyeOpenProbability}, rightEyeOpenProbability ${face.rightEyeOpenProbability}, $normalizeThreshold")
        return ((face.leftEyeOpenProbability <= normalizeThreshold)
                && (face.rightEyeOpenProbability <= normalizeThreshold))
    }

    fun resetDefaultSetting() {
        setSensitiveThresholdAlert(DEFAULT_SENSITIVE_THRESHOLD_ALERT)
        setDurationThresholdAlert(DEFAULT_DURATION_THRESHOLD_ALERT)
        setAlertEmptyFace(DEFAULT_EMPTY_FACE_ALERT)
    }
}