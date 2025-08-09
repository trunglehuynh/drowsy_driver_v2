package com.galaxylab.drowsydriver.UI.SoundPicker

import android.net.Uri

data class SoundItem(
    val title: String,
    val source: SoundSource,
    val uriString: String, // asset file name or content uri string
    val durationMs: Long
)

enum class SoundSource { ASSET, NOTIFICATION, RINGTONE }

