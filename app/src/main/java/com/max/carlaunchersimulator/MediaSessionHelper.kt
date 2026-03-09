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

/**
 * MediaSession 连接与控制封装。
 * 通过 MediaBrowser 连接指定音乐 App 的 MediaService，获取 MediaController 后提供播放/暂停/上下曲、
 * 当前歌曲与专辑图查询，以及重连与释放接口。供主界面等调用。
 */
class MediaSessionHelper(private val context: Context) {

    /** 系统 MediaSession 管理服务，用于获取/连接会话 */
    private var mediaSessionManager: MediaSessionManager? = null
    /** 连接成功后的媒体控制器，用于发送播放控制与获取元数据 */
    private var mediaController: MediaControllerCompat? = null
    /** 注册在 mediaController 上的回调，用于接收元数据与播放状态变化 */
    private var mediaControllerCallback: MediaControllerCallback? = null
    /** 用于连接音乐 App MediaService 的 MediaBrowser */
    private var mediaBrowser: MediaBrowserCompat? = null
    /** 当前是否处于播放中（根据 PlaybackState 维护） */
    private var isPlaying = false
    /** 当前歌曲信息（标题、艺术家、专辑图等），由元数据回调更新 */
    private var currentSong: Song? = null

    /**
     * 当前歌曲的简要数据。
     *
     * @param title    歌曲标题
     * @param artist   艺术家
     * @param uri      资源 URI（本示例未使用）
     * @param albumArt 专辑图，可为 null
     */
    data class Song(val title: String, val artist: String, val uri: String, val albumArt: android.graphics.Bitmap? = null)

    /**
     * 初始化 MediaSessionManager 并尝试连接音乐 App。
     * 失败时仅打日志，不抛异常。
     */
    fun initialize() {
        try {
            Log.d(TAG, "初始化MediaSessionManager...")
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            Log.d(TAG, "MediaSessionManager初始化成功")
            connectToMusicApp()
        } catch (e: Exception) {
            Log.e(TAG, "MediaSessionManager初始化失败: ${e.message}", e)
        }
    }

    /**
     * 使用 MediaBrowser 连接音乐 App 的 MediaService（包名/类名写死为 com.max.media_center）。
     * 连接结果通过 [connectionCallback] 异步回调。
     */
    private fun connectToMusicApp() {
        try {
            Log.d(TAG, "尝试使用MediaBrowser连接音乐App...")
            val serviceComponent = ComponentName("com.max.media_center", "com.max.media_center.MediaService")
            mediaBrowser = MediaBrowserCompat(context, serviceComponent, connectionCallback, null)
            mediaBrowser?.connect()
        } catch (e: SecurityException) {
            Log.e(TAG, "权限不足: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "连接失败: ${e.message}", e)
        }
    }

    /** MediaBrowser 连接状态回调：成功时创建 MediaController 并注册回调、拉取当前歌曲 */
    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "MediaBrowser连接成功！")
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

    /**
     * 创建并注册 [MediaControllerCallback]，用于接收元数据与播放状态变更。
     */
    private fun setupMediaControllerCallback() {
        val callback = MediaControllerCallback()
        mediaControllerCallback = callback
        mediaController?.registerCallback(callback)
    }

    /**
     * 从当前 MediaController 的 metadata 中读取标题、艺术家、专辑图并更新 [currentSong]。
     */
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

    /**
     * MediaController 回调：元数据或播放状态变化时更新内部状态（当前歌曲、是否播放中）。
     */
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

    /** 发送播放 */
    fun play() {
        mediaController?.transportControls?.play()
    }

    /** 发送暂停 */
    fun pause() {
        mediaController?.transportControls?.pause()
    }

    /** 下一曲 */
    fun next() {
        mediaController?.transportControls?.skipToNext()
    }

    /** 上一曲 */
    fun previous() {
        mediaController?.transportControls?.skipToPrevious()
    }

    /** 停止播放 */
    fun stop() {
        mediaController?.transportControls?.stop()
    }

    /**
     * 返回当前歌曲信息；未连接或无元数据时返回默认占位文案。
     *
     * @return 当前 [Song]，不会为 null
     */
    fun getCurrentSong(): Song = currentSong ?: Song(
        context.getString(R.string.media_no_song),
        context.getString(R.string.media_no_artist),
        "",
        null
    )

    /**
     * 返回当前歌曲专辑图。
     *
     * @return 专辑图 Bitmap，无则为 null
     */
    fun getAlbumArt(): android.graphics.Bitmap? = currentSong?.albumArt

    /** 当前是否处于播放状态 */
    fun isPlaying(): Boolean = isPlaying

    /** 是否已连接（即 mediaController 非 null） */
    fun isConnected(): Boolean = mediaController != null

    /**
     * 若未连接则再次尝试连接（用于定时重连）。
     */
    fun tryReconnect() {
        if (mediaController == null) {
            connectToMusicApp()
        }
    }

    /**
     * 释放资源：注销回调、断开 MediaBrowser、清空引用。应在 Activity onDestroy 等时机调用。
     */
    fun release() {
        mediaControllerCallback?.let { mediaController?.unregisterCallback(it) }
        mediaController = null
        mediaControllerCallback = null
        mediaBrowser?.disconnect()
        mediaBrowser = null
    }

    companion object {
        private const val TAG = "MediaSessionHelper"
    }
}
