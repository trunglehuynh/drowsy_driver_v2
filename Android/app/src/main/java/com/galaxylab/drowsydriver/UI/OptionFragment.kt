package com.galaxylab.drowsydriver.UI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.SeekBar
import android.media.RingtoneManager
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.AlertController
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.UserInfo.Companion.MAX_DURATION_THRESHOLD_ALERT
import com.galaxylab.drowsydriver.UserInfo.Companion.MAX_SENSITIVE_THRESHOLD_ALERT
import com.galaxylab.drowsydriver.UserInfo.Companion.MIN_DURATION_THRESHOLD_ALERT
import com.galaxylab.drowsydriver.UserInfo.Companion.MIN_SENSITIVE_THRESHOLD_ALERT
import com.galaxylab.drowsydriver.Utility.UpdateController
import com.galaxylab.drowsydriver.databinding.FragmentOptionBinding

import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import com.galaxylab.drowsydriver.UI.SoundPicker.SoundPickerViewModel
import timber.log.Timber


class OptionFragment : Fragment() {

    private val TAG = "OptionFragment"
    private val alertController: AlertController = get()
    private val updateController: UpdateController = get()
    private var _binding: FragmentOptionBinding? = null
    private val cameraFragVM: CameraFragmentVM by activityViewModel()
    private val soundPickerVM: SoundPickerViewModel by activityViewModel()

    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        initSpinnerSoundSelection()
//        initVolumeSeekBar()
        initSensitiveSeekBar()
        initDurationSeekBar()
        updateSelectedSoundTV()
        soundPickerVM.selected.observe(viewLifecycleOwner) { item ->
            val name = item?.title ?: getDisplayName(alertController.lastSoundName())
            binding.appCompatTextView.text = "Sound: $name"
        }

        binding.appCompatTextView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, com.galaxylab.drowsydriver.UI.SoundPicker.SoundPickerFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.resetDefaultBtn.setOnClickListener {
            AlertDialog.Builder(requireActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle("Reset Default Setting")
                .setMessage("Are you sure you want to reset all settings to their default values?\nThis action cannot be undone.")
                .setPositiveButton("OK") { _, _ ->
                    cameraFragVM.resetDefaultSetting()
                    binding.seekBarSensitive.progress = cameraFragVM.getSensitiveThresholdAlert()
                    binding.seekBarDuration.progress = cameraFragVM.getDurationThresholdAlert().toInt()
                    updateDurationTV()
                    binding.emptyFaceAlertSwitch.isChecked = cameraFragVM.getIsAlertEmptyFace()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        binding.startBtn.setOnClickListener { (context as MainActivity).mayOpenCameraFragment() }
        binding.menuBtn.setOnClickListener { showPopup(it) }
        binding.learnMoreIcon.setOnClickListener {
            Timber.d("tab on learnMoreTV")
            AlertDialog.Builder(requireActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setPositiveButton("OK") { _, _ -> }
                .setTitle("Sensitivity for Eye Size Accuracy")
                .setMessage("The AI detects drowsiness by analyzing human eye movement. Since eye sizes vary from person to person, consider adjusting the sensitivity settings if you experience frequent false alerts.")
                .create().show()
        }

        binding.durationLearnMoreIcon.setOnClickListener {
            Timber.d("tab on Time learnMoreTV")
            AlertDialog.Builder(requireActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setPositiveButton("OK") { _, _ -> }
                .setTitle("Eye-Closure Duration")
                .setMessage("The AI detects drowsiness based on how long your eyes remain closed. By default, the app triggers an alert if your eyes are closed more than half a second.\nHowever, since distinguishing between a blink and drowsiness can be challenging, you may want to increase the threshold if you encounter frequent false alerts.\nWarning: Increasing the duration may increase the risk of delayed alerts.")
                .create().show()
        }
        initEmptyFaceAlert()
    }


    private fun updateSelectedSoundTV() {
        try {
            val sound = alertController.lastSoundName()
            binding.appCompatTextView.text = "Sound: ${getDisplayName(sound)}"
        } catch (_: Exception) { }
    }

    private fun getDisplayName(sound: String): String {
        return try {
            if (sound.startsWith("content://") || sound.startsWith("file://") || sound.startsWith("android.resource://")) {
                val uri = Uri.parse(sound)
                val rt = RingtoneManager.getRingtone(requireContext(), uri)
                rt?.getTitle(requireContext()) ?: (uri.lastPathSegment ?: sound)
            } else {
                sound.substringBeforeLast('.')
            }
        } catch (e: Exception) {
            sound.substringBeforeLast('.')
        }
    }

    // Removed SharedPreferences listener

    private fun initEmptyFaceAlert() {
        binding.emptyFaceAlertSwitch.isChecked = cameraFragVM.getIsAlertEmptyFace()
        binding.emptyFaceAlertSwitch.setOnCheckedChangeListener { _, isCheck ->
            cameraFragVM.setAlertEmptyFace(
                isCheck
            )
        }
        binding.emptyFaceAlertLearnMoreIcon.setOnClickListener {
            Timber.d("tab on Time learnMoreTV")
            AlertDialog.Builder(requireActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setPositiveButton("OK") { _, _ -> }
                .setTitle("No Face Detected Alert")
                .setMessage("If the AI is unable to detect a face through the camera, it will trigger an alert. This feature prevents users from mounting their phone incorrectly.")
                .create().show()
        }
    }

//    private fun openMap(latitude: Double, longitude: Double) {
//        // Create a URI with the location data
////        val geoUri = Uri.parse("geo:$latitude,$longitude")
//
////        val geoUri =   Uri.parse("geo:0,0?q=1600 Amphitheatre Parkway, Mountain+View, California")
//        val geoUri =   Uri.parse("google.navigation:q=1600 Amphitheatre Parkway, Mountain+View, California")
//
//        // Create an Intent to view the location
//        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
//        mapIntent.setPackage("com.google.android.apps.maps")
//
//        // Ensure there's an app available to handle the Intent
//        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
//            startActivity(mapIntent)
//        }
//    }

    private fun showPopup(v: View?) {
        val popup = PopupMenu(context, v)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener {

            when (it.itemId) {

                R.id.menuUpdate -> {
                    updateController.openAppOnGooglePlay(requireActivity())
                    true
                }

                R.id.menuRate -> {
                    updateController.openAppOnGooglePlay(requireActivity())

                    true
                }

                R.id.menuShare -> {
                    updateController.shareApp(requireActivity())
                    true
                }

                R.id.menuMoreApp -> {
                    updateController.openAllAppOnGooglePlay(requireActivity())
                    true
                }

                R.id.menuFeedback -> {
                    updateController.sendFeedBack(requireActivity())
                    true
                }
            }
            false
        }
        popup.show()
    }


//    private fun initVolumeSeekBar() {
//        if (Firebase.remoteConfig.getBoolean("is_max_volume_enabled")) {
//            alertController.setVolume(alertController.maxVolume())
////            Timber.d("$TAG set volume to max")
//        }
//        binding.seekBarVolume.max = alertController.maxVolume()
//        binding.seekBarVolume.progress = alertController.currentVolume()
//        updateVolumeTV()
//        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                alertController.setVolume(progress)
//                updateVolumeTV()
//                Timber.d("setOnSeekBarChangeListener $progress")
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//
//        })
//    }
//
//    private fun updateVolumeTV() {
//        binding.volumeTV.text = "Volume: ${
//            (alertController.currentVolume().toFloat() / alertController.maxVolume() * 100).toInt()
//        }%"
//    }

    private fun initSensitiveSeekBar() {
        binding.seekBarSensitive.max = MAX_SENSITIVE_THRESHOLD_ALERT
        binding.seekBarSensitive.min = MIN_SENSITIVE_THRESHOLD_ALERT
        binding.seekBarSensitive.progress = cameraFragVM.getSensitiveThresholdAlert()
        updateSensitiveTV()
        binding.seekBarSensitive.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cameraFragVM.setSensitiveThresholdAlert(progress)
                updateSensitiveTV()
                Timber.d("cameraFragVM.thresholdAlert ${cameraFragVM.getSensitiveThresholdAlert()}")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }

    private fun updateDurationTV() {
        binding.durationTV.text = "Duration: ${
            String.format(
                "%.1f",
                (cameraFragVM.getDurationThresholdAlert() / 1000F)
            )
        } seconds"
    }

    private fun initDurationSeekBar() {
        binding.seekBarDuration.max = MAX_DURATION_THRESHOLD_ALERT.toInt()
        binding.seekBarDuration.min = MIN_DURATION_THRESHOLD_ALERT.toInt()
        binding.seekBarDuration.progress = cameraFragVM.getDurationThresholdAlert().toInt()
        updateDurationTV()
        binding.seekBarDuration.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cameraFragVM.setDurationThresholdAlert(progress.toLong())
                updateDurationTV()
                Timber.d("cameraFragVM.thresholdAlert ${cameraFragVM.getDurationThresholdAlert()}")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateSensitiveTV() {
        binding.sensitiveTV.text = "Sensitive: ${
            (cameraFragVM.getSensitiveThresholdAlert()
                .toFloat() / MAX_SENSITIVE_THRESHOLD_ALERT * 100).toInt()
        }%"
    }

//    private fun initSpinnerSoundSelection() {
//
//        val map = mapOf(
//            "Short Alert" to "short_alert.wav",
//            "Cartoon Alert" to "cartoon_alert.mp3",
//            "Men Alert" to "men_alert.wav",
//            "Notification Up Alert" to "notification_up.wav",
//            "Pup Alert" to "pup_alert.mp3",
//            "Red Alert" to "red_alert.wav",
//            "Sweet Alert" to "sweet_alert.wav"
//        )
//        val sounds = resources.getStringArray(R.array.sound_arrays)
//
//        val lastSoundUri = alertController.lastSoundName()
//        val lastKey = map.entries.first { it.value == lastSoundUri }.key
//        Timber.d("lastSoundUri $lastSoundUri lastKey $lastKey")
//
//        var index = 0
//        for ((i, sound) in sounds.withIndex()) {
//            if (lastKey == sound) {
//                index = i
//                break
//            }
//        }
//        binding.spinnerSoundSelection.setSelection(index)
//        binding.spinnerSoundSelection.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//
//                override fun onItemSelected(
//                    parent: AdapterView<*>?,
//                    view: View?,
//                    position: Int,
//                    id: Long
//                ) {
//                    val sound = parent?.getItemAtPosition(position).toString()
//                    Timber.d("select $sound to ${map[sound]}")
//                    alertController.setAlarmSound(map[sound] ?: error("cannot find sound URI"))
//                }
//            }
//    }

}
