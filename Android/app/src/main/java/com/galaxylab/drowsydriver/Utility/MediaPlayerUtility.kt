package com.galaxylab.drowsydriver.Utility

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build

fun MediaPlayer._SetAudioStreamType(volumeType: Int) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(volumeType)
            .build()

        setAudioAttributes(audioAttributes)
    } else {
        setAudioStreamType(volumeType)
    }
}