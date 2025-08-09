package com.galaxylab.drowsydriver.UI.SoundPicker

import android.app.Application
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxylab.drowsydriver.AlertController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundPickerViewModel(
    private val app: Application,
    private val alertController: AlertController
) : ViewModel() {

    private val _loading = MutableLiveData(false) // legacy, not used by pager
    val loading: LiveData<Boolean> = _loading

    private val itemsBySource = mutableMapOf<SoundSource, MutableLiveData<List<SoundItem>>>()
    private val loadingBySource = mutableMapOf<SoundSource, MutableLiveData<Boolean>>() 

    private val _selected = MutableLiveData<SoundItem?>(null)
    val selected: LiveData<SoundItem?> = _selected

    private var currentTab: SoundSource = SoundSource.ASSET

    fun initAndLoad() {
        ensureLoaded(SoundSource.ASSET)
    }

    fun switchTab(source: SoundSource) {
        if (currentTab == source) return
        currentTab = source
        ensureLoaded(source)
    }

    fun select(item: SoundItem) {
        _selected.postValue(item)
    }

    fun saveSelection() {
        _selected.value?.let {
            alertController.setAlarmSound(it.uriString)
        }
    }

    fun getItems(source: SoundSource): LiveData<List<SoundItem>> =
        itemsBySource.getOrPut(source) { MutableLiveData(emptyList()) }

    fun getLoading(source: SoundSource): LiveData<Boolean> =
        loadingBySource.getOrPut(source) { MutableLiveData(false) }

    fun ensureLoaded(source: SoundSource) {
        val itemsLive = getItems(source) as MutableLiveData<List<SoundItem>>
        val loadingLive = getLoading(source) as MutableLiveData<Boolean>
        if (loadingLive.value == true || !itemsLive.value.isNullOrEmpty()) return
        loadingLive.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val data = when (source) {
                SoundSource.ASSET -> loadAssetSounds()
                SoundSource.NOTIFICATION -> loadSystemSounds(RingtoneManager.TYPE_NOTIFICATION, SoundSource.NOTIFICATION)
                SoundSource.RINGTONE -> loadSystemSounds(RingtoneManager.TYPE_RINGTONE, SoundSource.RINGTONE)
            }
            val last = alertController.lastSoundName()
            val preselect = data.firstOrNull { it.uriString == last }
            itemsLive.postValue(data)
            if (_selected.value == null && preselect != null) {
                _selected.postValue(preselect)
            }
            loadingLive.postValue(false)
        }
    }

    private fun loadAssetSounds(): List<SoundItem> {
        val files = app.assets.list("")?.toList().orEmpty()
        val retriever = MediaMetadataRetriever()
        val items = mutableListOf<SoundItem>()
        for (file in files) {
            try {
                val afd = app.assets.openFd(file)
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                if (dur <= MIN_DURATION_MS) {
                    items.add(
                        SoundItem(
                            title = file.substringBeforeLast('.'),
                            source = SoundSource.ASSET,
                            uriString = file,
                            durationMs = dur
                        )
                    )
                }
            } catch (_: Exception) { }
        }
        return items.sortedBy { it.title.lowercase() }
    }

    private fun loadSystemSounds(type: Int, source: SoundSource): List<SoundItem> {
        val manager = RingtoneManager(app)
        manager.setType(type)
        val cursor = manager.cursor
        val items = mutableListOf<SoundItem>()
        val retriever = MediaMetadataRetriever()
        try {
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                val uri = manager.getRingtoneUri(cursor.position) ?: continue
                val dur = try {
                    retriever.setDataSource(app, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } catch (_: Exception) { 0L }
                if (dur <= MIN_DURATION_MS) {
                    items.add(
                        SoundItem(
                            title = title,
                            source = source,
                            uriString = uri.toString(),
                            durationMs = dur
                        )
                    )
                }
            }
        } finally {
            cursor.close()
        }
        return items.sortedBy { it.title.lowercase() }
    }

    companion object {
        private const val MIN_DURATION_MS = 5000L
    }
}
