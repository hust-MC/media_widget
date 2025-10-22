package com.max.carlaunchersimulator

import android.content.ComponentName
import android.content.Context
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.FileProvider
import java.io.File

class MediaSessionHelper(private val context: Context) {

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaController: MediaControllerCompat? = null
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var isPlaying = false
    private var currentSong: Song? = null

    data class Song(val title: String, val artist: String, val uri: String)

    fun initialize() {
        // 初始化MediaSessionManager
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        
        // 尝试连接到音乐播放App的MediaSession
        connectToMusicApp()
    }

    private fun connectToMusicApp() {
        try {
            // 获取活跃的媒体会话
            val activeSessions = mediaSessionManager?.getActiveSessions(
                ComponentName(context, MainActivity::class.java)
            )

            if (!activeSessions.isNullOrEmpty()) {
                // 连接到第一个活跃的媒体会话（通常是音乐播放App）
                val sessionController = activeSessions[0]
                val sessionToken = sessionController.sessionToken
                // 将 MediaSession.Token 转换为 MediaSessionCompat.Token
                val compatToken = MediaSessionCompat.Token.fromToken(sessionToken)
                mediaController = MediaControllerCompat(context, compatToken)

                // 设置回调以监听媒体信息变化
                setupMediaControllerCallback()

                // 更新当前歌曲信息
                updateCurrentSong()
            }
        } catch (e: SecurityException) {
            // 权限不足，无法访问其他App的MediaSession
            e.printStackTrace()
        }
    }

    private fun setupMediaControllerCallback() {
        mediaControllerCallback = MediaControllerCallback()
        mediaController?.registerCallback(mediaControllerCallback!!)
    }

    private fun updateCurrentSong() {
        mediaController?.let { controller ->
            val metadata = controller.metadata
            if (metadata != null) {
                val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "未知歌曲"
                val artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "未知艺术家"
                currentSong = Song(title, artist, "")
            }
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            updateCurrentSong()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let {
                isPlaying = it.state == PlaybackStateCompat.STATE_PLAYING
            }
        }
    }

    fun play() {
        mediaController?.transportControls?.play()
    }

    fun pause() {
        mediaController?.transportControls?.pause()
    }

    fun next() {
        mediaController?.transportControls?.skipToNext()
    }

    fun previous() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun stop() {
        mediaController?.transportControls?.stop()
    }

    fun getCurrentSong(): Song = currentSong ?: Song("无歌曲", "无艺术家", "")

    fun isPlaying(): Boolean = isPlaying

    fun isConnected(): Boolean = mediaController != null

    fun tryReconnect() {
        if (mediaController == null) {
            connectToMusicApp()
        }
    }

    fun release() {
        mediaController?.unregisterCallback(mediaControllerCallback!!)
        mediaController = null
        mediaControllerCallback = null
    }
}
