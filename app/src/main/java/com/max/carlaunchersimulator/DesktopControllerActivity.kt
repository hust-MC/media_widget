package com.max.carlaunchersimulator

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 桌面端 MediaSession 控制页。
 * 通过 MediaSessionManager.getActiveSessions 获取当前活跃媒体会话并连接，展示歌曲信息与上一曲/播放暂停/下一曲控制。
 * 需 MEDIA_CONTENT_CONTROL 等权限；连接前控制按钮禁用，连接后更新元数据与播放状态。
 */
class DesktopControllerActivity : AppCompatActivity() {

    /** 系统 MediaSession 管理服务，获取活跃会话列表；初始化失败时为 null */
    private var mediaSessionManager: MediaSessionManager? = null
    /** 连接到的媒体控制器（framework 层），用于控制与获取元数据 */
    private var mediaController: MediaController? = null
    /** 注册在 mediaController 上的回调（当前为空实现，可扩展以刷新 UI） */
    private var mediaControllerCallback: MediaControllerCallback? = null

    /** 专辑图 */
    private lateinit var albumArt: ImageView
    /** 歌曲标题 */
    private lateinit var songTitle: TextView
    /** 歌手 */
    private lateinit var songArtist: TextView
    /** 上一曲按钮 */
    private lateinit var prevButton: ImageButton
    /** 播放/暂停按钮 */
    private lateinit var playPauseButton: ImageButton
    /** 下一曲按钮 */
    private lateinit var nextButton: ImageButton
    /** 连接状态文案（如已连接/未连接/错误信息） */
    private lateinit var statusText: TextView
    /** 点击后执行 connectToMediaSession 的连接按钮 */
    private lateinit var connectButton: Button

    /**
     * 创建时加载布局、初始化视图与 MediaSessionManager，并设置连接按钮与控制按钮点击事件。
     *
     * @param savedInstanceState 若由系统重建则非 null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desktop_controller)

        initializeViews()
        // 获取 MediaSessionManager，失败时在 statusText 提示
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        } catch (e: Exception) {
            Log.e(TAG, "获取 MediaSessionManager 失败", e)
            statusText.text = getString(R.string.desktop_status_init_failed, e.message.orEmpty())
        }
        connectButton.setOnClickListener { connectToMediaSession() }
        setupControlButtons()
    }

    /**
     * 绑定布局中的控件并设置初始状态：控制按钮禁用，状态为“未连接”。
     */
    private fun initializeViews() {
        albumArt = findViewById(R.id.desktop_album_art)
        songTitle = findViewById(R.id.desktop_song_title)
        songArtist = findViewById(R.id.desktop_song_artist)
        prevButton = findViewById(R.id.desktop_prev_button)
        playPauseButton = findViewById(R.id.desktop_play_pause_button)
        nextButton = findViewById(R.id.desktop_next_button)
        statusText = findViewById(R.id.desktop_status_text)
        connectButton = findViewById(R.id.connect_button)

        setControlButtonsEnabled(false)
        statusText.text = getString(R.string.desktop_status_disconnected)
    }

    /**
     * 为上一曲、播放/暂停、下一曲设置点击事件，通过 [mediaController] 的 transportControls 发送控制。
     */
    private fun setupControlButtons() {
        prevButton.setOnClickListener {
            mediaController?.transportControls?.skipToPrevious()
        }

        playPauseButton.setOnClickListener {
            mediaController?.let { controller ->
                if (isPlaying()) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
        }

        nextButton.setOnClickListener {
            mediaController?.transportControls?.skipToNext()
        }
    }

    /**
     * 使用 [mediaSessionManager] 获取以本 Activity 为监听组件的活跃会话，连接第一个并更新 UI。
     * 若 Manager 未初始化或无权限等，在 [statusText] 显示相应提示。
     */
    private fun connectToMediaSession() {
        val manager = mediaSessionManager ?: run {
            statusText.text = getString(R.string.desktop_status_manager_not_init)
            return
        }
        try {
            val activeSessions = manager.getActiveSessions(
                ComponentName(this, DesktopControllerActivity::class.java)
            )
            if (activeSessions.isNotEmpty()) {
                val sessionController = activeSessions[0]
                mediaController = MediaController(this, sessionController.sessionToken)
                setupMediaControllerCallback()
                setControlButtonsEnabled(true)
                statusText.text = getString(R.string.desktop_status_connected)
                updateMediaInfo()
            } else {
                statusText.text = getString(R.string.desktop_status_no_active_session)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "权限被拒绝", e)
            statusText.text = getString(R.string.desktop_status_permission_denied, e.message.orEmpty())
        } catch (e: Exception) {
            Log.e(TAG, "连接 MediaSession 失败", e)
            statusText.text = getString(R.string.desktop_status_connect_failed, e.message.orEmpty())
        }
    }

    /**
     * 创建并注册 [MediaControllerCallback] 到当前 [mediaController]。
     */
    private fun setupMediaControllerCallback() {
        val callback = MediaControllerCallback()
        mediaControllerCallback = callback
        mediaController?.registerCallback(callback)
    }

    /**
     * 从当前 [mediaController] 的 metadata 更新歌曲标题、艺术家，并刷新播放状态图标。
     */
    private fun updateMediaInfo() {
        mediaController?.let { controller ->
            val metadata = controller.metadata
            if (metadata != null) {
                songTitle.text = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: getString(R.string.desktop_default_title)
                songArtist.text = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: getString(R.string.desktop_default_artist)
            }
            updatePlaybackState()
        }
    }

    /**
     * 根据当前 [mediaController] 的 playbackState 更新播放/暂停按钮图标。
     */
    private fun updatePlaybackState() {
        mediaController?.let { controller ->
            val playbackState = controller.playbackState
            if (playbackState != null) {
                val isPlaying = playbackState.state == PlaybackStateCompat.STATE_PLAYING
                playPauseButton.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                )
            }
        }
    }

    /**
     * 当前是否处于播放状态。
     *
     * @return 若 playbackState 为 STATE_PLAYING 则 true，否则 false
     */
    private fun isPlaying(): Boolean {
        val playbackState = mediaController?.playbackState
        return playbackState?.state == PlaybackStateCompat.STATE_PLAYING
    }

    /**
     * 统一设置上一曲、播放/暂停、下一曲的可用状态与透明度。
     *
     * @param enabled true 为可点击且不透明，false 为禁用且半透明
     */
    private fun setControlButtonsEnabled(enabled: Boolean) {
        prevButton.isEnabled = enabled
        playPauseButton.isEnabled = enabled
        nextButton.isEnabled = enabled

        prevButton.alpha = if (enabled) 1.0f else 0.5f
        playPauseButton.alpha = if (enabled) 1.0f else 0.5f
        nextButton.alpha = if (enabled) 1.0f else 0.5f
    }

    /** 空实现，可在此监听元数据/播放状态变化并调用 [updateMediaInfo] / [updatePlaybackState] */
    private inner class MediaControllerCallback : MediaController.Callback() {

    }

    /**
     * 销毁时注销回调并清空 [mediaController]。
     */
    override fun onDestroy() {
        super.onDestroy()
        mediaControllerCallback?.let { mediaController?.unregisterCallback(it) }
        mediaController = null
    }

    companion object {
        private const val TAG = "DesktopControllerActivity"
    }

}
