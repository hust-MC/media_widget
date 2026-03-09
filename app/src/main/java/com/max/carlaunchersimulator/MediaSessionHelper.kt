package com.max.carlaunchersimulator

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.util.Log
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.max.carlaunchersimulator.R

class MediaSessionHelper(private val context: Context) {

    companion object {
        private const val TAG = "MediaSessionHelper"
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaController: MediaControllerCompat? = null
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var mediaBrowser: MediaBrowserCompat? = null
    private var isPlaying = false
    private var currentSong: Song? = null

    data class Song(val title: String, val artist: String, val uri: String, val albumArt: android.graphics.Bitmap? = null)

    fun initialize() {
        try {
            Log.d(TAG, "初始化MediaSessionManager...")
            // 初始化MediaSessionManager
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            Log.d(TAG, "MediaSessionManager初始化成功")
            
            // 尝试连接到音乐播放App的MediaSession
            connectToMusicApp()
        } catch (e: Exception) {
            Log.e(TAG, "MediaSessionManager初始化失败: ${e.message}", e)
        }
    }

    private fun connectToMusicApp() {
        try {
            Log.d(TAG, "尝试使用MediaBrowser连接音乐App...")
            
            // 使用MediaBrowser连接音乐App
            val serviceComponent = ComponentName("com.max.media_center", "com.max.media_center.MediaService")
            mediaBrowser = MediaBrowserCompat(context, serviceComponent, connectionCallback, null)
            mediaBrowser?.connect()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "权限不足: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "连接失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "MediaBrowser连接成功！")
            
            // 获取MediaSession Token
            val sessionToken = mediaBrowser?.sessionToken
            if (sessionToken != null) {
                mediaController = MediaControllerCompat(context, sessionToken)
                setupMediaControllerCallback()
                updateCurrentSong()
                Log.d(TAG, "MediaController设置成功！")
            }
        }
        
        override fun onConnectionFailed() {
            Log.e(TAG, "MediaBrowser连接失败")
        }
        
        override fun onConnectionSuspended() {
            Log.d(TAG, "MediaBrowser连接暂停")
        }
    }

    private fun setupMediaControllerCallback() {
        val callback = MediaControllerCallback()
        mediaControllerCallback = callback
        mediaController?.registerCallback(callback)
    }

    private fun updateCurrentSong() {
        mediaController?.let { controller ->
            val metadata = controller.metadata
            if (metadata != null) {
                val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: context.getString(R.string.media_unknown_song)
                val artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: context.getString(R.string.media_unknown_artist)
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

    fun getCurrentSong(): Song = currentSong ?: Song(
        context.getString(R.string.media_no_song),
        context.getString(R.string.media_no_artist),
        "",
        null
    )
    
    fun getAlbumArt(): android.graphics.Bitmap? = currentSong?.albumArt

    fun isPlaying(): Boolean = isPlaying

    fun isConnected(): Boolean = mediaController != null

    fun tryReconnect() {
        if (mediaController == null) {
            connectToMusicApp()
        }
    }

    fun release() {
        mediaControllerCallback?.let { mediaController?.unregisterCallback(it) }
        mediaController = null
        mediaControllerCallback = null
        mediaBrowser?.disconnect()
        mediaBrowser = null
    }
}
