package com.yb.part4_chapter02

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.yb.part4_chapter02.databinding.FragmentPlayerBinding
import com.yb.part4_chapter02.service.MusicDTO
import com.yb.part4_chapter02.model.MusicModel
import com.yb.part4_chapter02.model.PlayerModel
import com.yb.part4_chapter02.service.MusicService
import com.yb.part4_chapter02.service.mapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var playerModel: PlayerModel = PlayerModel()
    private var binding: FragmentPlayerBinding? = null

    private var player: ExoPlayer? = null
    private lateinit var playlistAdapter: PlaylistAdapter

    private val updateSeekRunnable: Runnable = Runnable {
        updateSeek()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentPlayerBinding = FragmentPlayerBinding.bind(view)
        binding = fragmentPlayerBinding

        initPlayerView(fragmentPlayerBinding)
        initPlaylistButton(fragmentPlayerBinding)
        initPlayControlButtons(fragmentPlayerBinding)
        initSeekBar(fragmentPlayerBinding)
        initPlaylistRecyclerView(fragmentPlayerBinding)

        getMusicListFromServer()
    }

    private fun initPlayerView(fragmentPlayerBinding: FragmentPlayerBinding) {
        context?.let {
            player = ExoPlayer.Builder(it).build()
        }
        fragmentPlayerBinding.playerView.player = player

        binding?.let {
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        it.playControlImageView.setImageResource(R.drawable.ic_baseline_pause_48)
                    } else {
                        it.playControlImageView.setImageResource(R.drawable.ic_baseline_play_arrow_48)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)

                    val newIndex = mediaItem?.mediaId ?: return
                    playerModel.currentPosition = newIndex.toInt()
                    updatePlayerView(playerModel.currentMusicModel())
                    playlistAdapter.submitList(playerModel.getAdapterModels())
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)

                    updateSeek()
                }
            })
        }
    }

    private fun updateSeek() {
        val player = this.player ?: return
        val duration = if (player.duration > 0) player.duration else 0
        val position = player.currentPosition

        updateSeekUI(duration, position)

        val state = player.playbackState

        view?.removeCallbacks(updateSeekRunnable)
        if (state != Player.STATE_IDLE && state != Player.STATE_ENDED) {    // 재생중
            view?.postDelayed(updateSeekRunnable, 1000)
        }
    }

    private fun updateSeekUI(duration: Long, position: Long) {
        binding?.let {
            it.playlistSeekBar.max = (duration / 1000).toInt()
            it.playlistSeekBar.progress = (position / 1000).toInt()
            it.playerSeekBar.max = (duration / 1000).toInt()
            it.playerSeekBar.progress = (position / 1000).toInt()
            it.playTimeTextView.text = String.format("%02d:%02d",
                TimeUnit.MINUTES.convert(position, TimeUnit.MILLISECONDS),
                (position / 1000) % 60)
            it.totalTimeTextView.text = String.format("%02d:%02d",
                TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS),
                (duration / 1000) % 60)
        }
    }

    private fun updatePlayerView(currentMusicModel: MusicModel?) {
        currentMusicModel ?: return

        binding?.let {
            it.trackTextView.text = currentMusicModel.track
            it.artistTextView.text = currentMusicModel.artist

            Glide.with(it.coverImageView.context)
                .load(currentMusicModel.coverUrl)
                .into(it.coverImageView)
        }
    }

    private fun initPlaylistButton(fragmentPlayerBinding: FragmentPlayerBinding) {
        //TODO 선택된 데이터가 없는 경우, 서버에서 데이터를 불러오지 못한 경우 예외처리

        fragmentPlayerBinding.playlistImageView.setOnClickListener {
            if (playerModel.currentPosition == -1) return@setOnClickListener

            fragmentPlayerBinding.playerViewGroup.isVisible = playerModel.isWatchingPlayListView
            fragmentPlayerBinding.playlistViewGroup.isVisible =
                playerModel.isWatchingPlayListView.not()

            playerModel.isWatchingPlayListView = playerModel.isWatchingPlayListView.not()
        }
    }

    private fun initPlayControlButtons(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playControlImageView.setOnClickListener {
            val player = this.player ?: return@setOnClickListener

            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }

        }

        fragmentPlayerBinding.skipNextImageView.setOnClickListener {
            val nextMusicModel = playerModel.nextMusic() ?: return@setOnClickListener
            playMusic(nextMusicModel)
        }

        fragmentPlayerBinding.skipPrevImageView.setOnClickListener {
            val prevMusicModel = playerModel.prevMusic() ?: return@setOnClickListener
            playMusic(prevMusicModel)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSeekBar(fragmentPlayerBinding: FragmentPlayerBinding) {
        fragmentPlayerBinding.playerSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                player?.seekTo((seekBar.progress * 1000).toLong())
            }

        })

        fragmentPlayerBinding.playlistSeekBar.setOnTouchListener { v, event ->
            false
        }
    }

    private fun initPlaylistRecyclerView(fragmentPlayerBinding: FragmentPlayerBinding) {
        playlistAdapter = PlaylistAdapter {
            playMusic(it)
        }
        fragmentPlayerBinding.playlistRecyclerView.layoutManager = LinearLayoutManager(context)
        fragmentPlayerBinding.playlistRecyclerView.adapter = playlistAdapter

    }

    private fun getMusicListFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(MusicService::class.java)
            .also { musicService ->
                musicService.listMusics().enqueue(object : Callback<MusicDTO> {
                    override fun onResponse(call: Call<MusicDTO>, response: Response<MusicDTO>) {
                        if (response.isSuccessful.not()) {
                            return
                        }

                        response.body()?.let {
                            playerModel = it.mapper()
                            setMusicList(playerModel.getAdapterModels())
                            playlistAdapter.submitList(playerModel.getAdapterModels())
                        }

                        Log.d("PlayerFragment", response.body().toString())
                    }

                    override fun onFailure(call: Call<MusicDTO>, t: Throwable) {
                        // 예외처리
                    }
                })
            }
    }

    private fun setMusicList(musicModelList: List<MusicModel>) {
        context?.let { context ->
            player?.addMediaItems(musicModelList.map {
                MediaItem.Builder()
                    .setMediaId(it.id.toString())
                    .setUri(it.streamUrl)
                    .build()
            })

            player?.prepare()
        }
    }

    private fun playMusic(musicModel: MusicModel) {
        playerModel.updateCurrentPosition(musicModel)
        player?.seekTo(playerModel.currentPosition, 0)
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
        view?.removeCallbacks(updateSeekRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        binding = null
        player?.release()
        view?.removeCallbacks(updateSeekRunnable)
    }

    companion object {
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }
}