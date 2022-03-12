package com.yb.part4_chapter02.model

import com.google.gson.annotations.SerializedName

data class MusicModel(
    val id: Long,
    val track: String,
    val streamUrl: String,
    val artist: String,
    val coverUrl: String,
    val isPlaying: Boolean = false
)
