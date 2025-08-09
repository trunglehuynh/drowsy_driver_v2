package com.galaxylab.drowsydriver.UI.SoundPicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.galaxylab.drowsydriver.R

class SoundItemAdapter(
    private val onItemClick: (SoundItem) -> Unit
) : ListAdapter<SoundItem, SoundItemAdapter.VH>(DIFF) {

    private var selectedUri: String? = null

    fun setSelectedUri(uri: String?) {
        selectedUri = uri
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sound, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectedUri, onItemClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.soundTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.soundSubtitle)
        private val check: ImageView = itemView.findViewById(R.id.soundChecked)

        fun bind(item: SoundItem, selectedUri: String?, onItemClick: (SoundItem) -> Unit) {
            title.text = item.title
            subtitle.text = String.format("%.1f s", item.durationMs / 1000f)
            check.visibility = if (item.uriString == selectedUri) View.VISIBLE else View.INVISIBLE
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SoundItem>() {
            override fun areItemsTheSame(oldItem: SoundItem, newItem: SoundItem): Boolean =
                oldItem.uriString == newItem.uriString

            override fun areContentsTheSame(oldItem: SoundItem, newItem: SoundItem): Boolean =
                oldItem == newItem
        }
    }
}

