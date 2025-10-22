package com.max.carlaunchersimulator

import android.content.ComponentName
import android.content.Context
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
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
    private var mediaBrowser: MediaBrowserCompat? = null
    private var isPlaying = false
    private var currentSong: Song? = null

    data class Song(val title: String, val artist: String, val uri: String, val albumArt: android.graphics.Bitmap? = null)

    fun initialize() {
        try {
            println("🔧 初始化MediaSessionManager...")
            // 初始化MediaSessionManager
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            println("✅ MediaSessionManager初始化成功")
            
            // 尝试连接到音乐播放App的MediaSession
            connectToMusicApp()
        } catch (e: Exception) {
            println("❌ MediaSessionManager初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun connectToMusicApp() {
        try {
            println("🔍 尝试使用MediaBrowser连接音乐App...")
            
            // 使用MediaBrowser连接音乐App
            val serviceComponent = ComponentName("com.max.media_center", "com.max.media_center.MediaService")
            mediaBrowser = MediaBrowserCompat(context, serviceComponent, connectionCallback, null)
            mediaBrowser?.connect()
            
        } catch (e: SecurityException) {
            println("❌ 权限不足: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            println("❌ 连接失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            println("✅ MediaBrowser连接成功！")
            
            // 获取MediaSession Token
            val sessionToken = mediaBrowser?.sessionToken
            if (sessionToken != null) {
                mediaController = MediaControllerCompat(context, sessionToken)
                setupMediaControllerCallback()
                updateCurrentSong()
                println("✅ MediaController设置成功！")
            }
        }
        
        override fun onConnectionFailed() {
            println("❌ MediaBrowser连接失败")
        }
        
        override fun onConnectionSuspended() {
            println("⚠️ MediaBrowser连接暂停")
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
                val albumArt = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
                currentSong = Song(title, artist, "", albumArt)
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

    fun getCurrentSong(): Song = currentSong ?: Song("无歌曲", "无艺术家", "", null)
    
    fun getAlbumArt(): android.graphics.Bitmap? = currentSong?.albumArt

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
        mediaBrowser?.disconnect()
        mediaBrowser = null
    }
}
