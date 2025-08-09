package com.galaxylab.drowsydriver.UI

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.AI.CameraConfiguration
import com.galaxylab.drowsydriver.AI.DetectedFaces
import com.galaxylab.drowsydriver.AI.FaceGraphic
import com.galaxylab.drowsydriver.BuildConfig
import com.galaxylab.drowsydriver.databinding.FragmentCameraBinding
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    companion object {
        const val DESTINATION_ADDRESS_KEY = "DESTINATION_ADDRESS_KEY"
        private const val TAG = "CameraFragment"
    }

    private val viewModel: CameraFragmentVM by activityViewModel()
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var mlExecutor: ExecutorService? = null
    private var isLandscapeMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getDetectedFaces().observe(viewLifecycleOwner) {
            drawFaceBoundingBox(it, isLandscapeMode)
            if (BuildConfig.DEBUG) {
                binding.debugImageView.setImageBitmap(it.bitmap)
            }
            Timber.d("$TAG , bitmap width ${it.bitmap.width}, height ${it.bitmap.height} face ${it.face}")
            binding.graphicOverlay.setCameraInfo(
                it.bitmap.width, it.bitmap.height, CameraConfiguration.facingLen
            )
        }

        viewModel.getClosedEyeAlertEvent().observe(viewLifecycleOwner) {
            binding.alertImage.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.getEmptyFaceAlertEvent().observe(viewLifecycleOwner) {
            binding.noFaceAlertTV.visibility = if (it) View.VISIBLE else View.GONE
        }

        binding.PIPBtn.setOnClickListener {
            activity?.apply {
                val aspectRatio = Rational(16, 9)
                val pictureInPictureParam =
                    PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                setPictureInPictureParams(pictureInPictureParam)
                enterPictureInPictureMode(pictureInPictureParam)
            }
        }
        maybeOpenGoogleMap()
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val targetResolution =
            if (isPortrait) CameraConfiguration.portraitSize else CameraConfiguration.landscapeSize
        startCamera(targetResolution)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        Timber.d("newConfig.orientation ${newConfig.orientation}")
        isLandscapeMode = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (newConfig.orientation != Configuration.ORIENTATION_PORTRAIT && newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE) return
        val isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        val targetResolution =
            if (isPortrait) CameraConfiguration.portraitSize else CameraConfiguration.landscapeSize
        startCamera(targetResolution)
    }

    override fun onDestroy() {
        super.onDestroy()
        mlExecutor?.shutdownNow()
        mlExecutor = null
    }

    private fun maybeOpenGoogleMap(): Boolean {
        val address = arguments?.getString(DESTINATION_ADDRESS_KEY) ?: return false
        activity?.apply {
            val aspectRatio = Rational(16, 9)
            val pictureInPictureParam =
                PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
            setPictureInPictureParams(pictureInPictureParam)
            enterPictureInPictureMode(pictureInPictureParam)
        }
        openMap(address)
        return true
    }

    private fun openMap(address: String) {
//        val geoUri =   Uri.parse("geo:0,0?q=1600 Amphitheatre Parkway, Mountain+View, California")
        val geoUri = Uri.parse("google.navigation:q=$address")
        // Create an Intent to view the location
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        // Ensure there's an app available to handle the Intent
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        }
    }

//    private val LAST_TIME_SHOW_ADS = "LAST_TIME_SHOW_ADS"
//    private val FIVE_MINUTES = 1000 * 60 * 5
//    private fun showAds() {
//
//        val current = System.currentTimeMillis()
//        val lastTimeShow = sharedPreferences.getLong(LAST_TIME_SHOW_ADS, current + FIVE_MINUTES)
//        if (current < lastTimeShow) return
//
//        sharedPreferences.edit().putLong(LAST_TIME_SHOW_ADS, current + FIVE_MINUTES).apply()
//
//        val mInterstitialAd = InterstitialAd(context)
//        if (BuildConfig.DEBUG)
//            mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712" // testing AdUnitID
//        else // release version
//            mInterstitialAd.adUnitId = "ca-app-pub-2331141815165218/4762723623"
//
//
//        mInterstitialAd.adListener = object : AdListener() {
//            override fun onAdLoaded() {
//                mInterstitialAd.show()
//                Timber.d("show ads")
//            }
//
//            override fun onAdFailedToLoad(p0: LoadAdError?) {
//                p0?.apply { Timber.i(this.message) }
//            }
//
//        }
//        mInterstitialAd.loadAd(AdRequest.Builder().build())
//    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        binding.PIPBtn.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
    }


    private fun drawFaceBoundingBox(detectedFaces: DetectedFaces, isLandscapeMode: Boolean) {
        binding.graphicOverlay.clear()

        binding.graphicOverlay.add(
            FaceGraphic(
                binding.graphicOverlay, detectedFaces.face, isLandscapeMode, null
            )
        )
        binding.graphicOverlay.invalidate()
    }


    private fun startCamera(targetResolution: Size) {
//        val targetResolution =
//            if (isPortrait) CameraConfiguration.portraitSize else CameraConfiguration.landscapeSize
        mlExecutor?.shutdownNow()
        mlExecutor = Executors.newSingleThreadExecutor()
        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(
                targetResolution,  // preferred resolution
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        ).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

            val imageAnalysis = ImageAnalysis.Builder().setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            imageAnalysis.setAnalyzer(mlExecutor!!, viewModel)

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraConfiguration.facingLen).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis, preview
                )
                preview.surfaceProvider = binding.viewFinder.surfaceProvider
            } catch (e: Exception) {
                Timber.e("startCamera $e")
                Firebase.crashlytics.recordException(e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }
}