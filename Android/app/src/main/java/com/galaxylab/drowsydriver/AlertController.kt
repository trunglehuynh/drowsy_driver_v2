package com.galaxylab.drowsydriver

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.media.AudioManager
import android.media.MediaPlayer
import android.widget.Toast
import com.galaxylab.drowsydriver.Utility._SetAudioStreamType
import timber.log.Timber

class AlertController(
    private val applicationContext: Context,
    private val sharedPreferences: SharedPreferences,
    private val enable: Boolean = true
) {
    private lateinit var mediaPlayer: MediaPlayer
    private val audioManager: AudioManager
    private val AudioType = AudioManager.STREAM_MUSIC

    private val LAST_SOUND_NAME = "LAST_SOUND_URL"
    private val DEAULT_SOUND = "short_alert.wav"

    init {
//        val lastSoundName = sharedPreferences.getString(LAST_SOUND_NAME, DEAULT_SOUND)!!
//        setAlarmSound(lastSoundName)
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setAlarmSound(lastSoundName())
    }

    fun setAlarmSound(sound: String) {
        mediaPlayer = MediaPlayer()
        try {
            if (sound.startsWith("content://") || sound.startsWith("file://") || sound.startsWith("android.resource://")) {
                mediaPlayer.setDataSource(applicationContext, android.net.Uri.parse(sound))
            } else {
                val afd = getAssetFile(sound)
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mediaPlayer.prepare()
            mediaPlayer._SetAudioStreamType(AudioType)
            sharedPreferences.edit().putString(LAST_SOUND_NAME, sound).apply()

        } catch (e: Exception) {
            Timber.e(e)
            // fallback to default
            val afd = getAssetFile(DEAULT_SOUND)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mediaPlayer.prepare()
            mediaPlayer._SetAudioStreamType(AudioType)
        }
    }

    fun getAssetFile(sound: String): AssetFileDescriptor {
        try {
            return applicationContext.assets.openFd(sound)
        } catch (e: Exception) {
            Timber.e(e)
            return applicationContext.assets.openFd(DEAULT_SOUND)
        }
    }

    fun lastSoundName(): String = sharedPreferences.getString(LAST_SOUND_NAME, DEAULT_SOUND)!!

    fun maxVolume() = audioManager.getStreamMaxVolume(AudioType)

    fun currentVolume(): Int = audioManager.getStreamVolume(AudioType)

    fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioType, volume, 0)
    }

    fun alert() {
        if (!enable) return
        if (mediaPlayer.isPlaying) return
        if (currentVolume() == 0) {
            Toast.makeText(
                applicationContext,
                "Volume is mute",
                Toast.LENGTH_LONG
            ).show()
        }
        mediaPlayer.start()
    }
}
