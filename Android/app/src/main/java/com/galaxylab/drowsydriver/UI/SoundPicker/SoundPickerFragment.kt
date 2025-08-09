package com.galaxylab.drowsydriver.UI.SoundPicker

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.galaxylab.drowsydriver.AlertController
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.Utility._SetAudioStreamType
import com.galaxylab.drowsydriver.databinding.FragmentSoundPickerBinding
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class SoundPickerFragment : Fragment() {

    private var _binding: FragmentSoundPickerBinding? = null
    private val binding get() = _binding!!
    private val alertController: AlertController = get()
    private val viewModel: SoundPickerViewModel by activityViewModel()

    private var mediaPlayer: MediaPlayer? = null
    

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoundPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewPager with 3 tabs
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int) = when (position) {
                0 -> SoundListFragment.newInstance(SoundSource.ASSET)
                1 -> SoundListFragment.newInstance(SoundSource.NOTIFICATION)
                else -> SoundListFragment.newInstance(SoundSource.RINGTONE)
            }
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_assets)
                1 -> getString(R.string.tab_notifications)
                else -> getString(R.string.tab_ringtones)
            }
        }.attach()

        setupVolume()
        binding.btnSelect.setOnClickListener {
            viewModel.saveSelection()
            parentFragmentManager.popBackStack()
        }

        viewModel.selected.observe(viewLifecycleOwner) { item ->
            binding.btnSelect.isEnabled = item != null
        }

        viewModel.initAndLoad()
    }

    fun onItemSelectedFromChild(item: SoundItem) {
        viewModel.select(item)
        playPreview(item)
    }

    private fun setupVolume() {
        binding.seekBarVolume.max = alertController.maxVolume()
        binding.seekBarVolume.progress = alertController.currentVolume()
        updateVolumeTV()
        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                alertController.setVolume(progress)
                updateVolumeTV()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateVolumeTV() {
        val percent = (alertController.currentVolume().toFloat() / alertController.maxVolume() * 100).toInt()
        binding.volumeTV.text = getString(R.string.volume_percent, percent)
    }

    private fun playPreview(item: SoundItem) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            val mp = mediaPlayer!!
            when (item.source) {
                SoundSource.ASSET -> {
                    val afd = requireContext().assets.openFd(item.uriString)
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                SoundSource.NOTIFICATION, SoundSource.RINGTONE -> {
                    mp.setDataSource(requireContext(), Uri.parse(item.uriString))
                }
            }
            mp._SetAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}
