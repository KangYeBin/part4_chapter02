package com.yb.part4_chapter02.service

import com.yb.part4_chapter02.model.MusicEntity
import com.yb.part4_chapter02.model.MusicModel
import com.yb.part4_chapter02.model.PlayerModel

fun MusicEntity.mapper(id: Long): MusicModel {
    return MusicModel(id = id,
        track = this.track,
        streamUrl = this.streamUrl,
        artist = this.artist,
        coverUrl = this.coverUrl
    )
}

fun MusicDTO.mapper(): PlayerModel =
    PlayerModel(
        playMusicList = musics.mapIndexed { index, musicEntity ->
            musicEntity.mapper(index.toLong())
        }
    )