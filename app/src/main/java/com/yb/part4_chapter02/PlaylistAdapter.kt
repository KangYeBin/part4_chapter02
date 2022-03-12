package com.yb.part4_chapter02

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yb.part4_chapter02.databinding.ItemMusicBinding
import com.yb.part4_chapter02.model.MusicModel

class PlaylistAdapter(private val callback: (MusicModel) -> Unit) :
    ListAdapter<MusicModel, PlaylistAdapter.ViewHolder>(diffUtil) {
    private var binding: ItemMusicBinding? = null

    inner class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(musicModel: MusicModel) {
            val itemMusicBinding = ItemMusicBinding.bind(view)
            binding = itemMusicBinding

            itemMusicBinding.itemTrackTextView.text = musicModel.track
            itemMusicBinding.itemArtistTextView.text = musicModel.artist

            Glide.with(itemMusicBinding.itemCoverImageView.context)
                .load(musicModel.coverUrl)
                .into(itemMusicBinding.itemCoverImageView)

            if (musicModel.isPlaying) {
                itemView.setBackgroundColor(Color.GRAY)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                callback(musicModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<MusicModel>() {
            override fun areItemsTheSame(oldItem: MusicModel, newItem: MusicModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MusicModel, newItem: MusicModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}