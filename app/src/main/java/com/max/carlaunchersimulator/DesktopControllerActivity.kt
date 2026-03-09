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

class DesktopControllerActivity : AppCompatActivity() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaController: MediaController? = null
    private var mediaControllerCallback: MediaControllerCallback? = null

    // UI Components
    private lateinit var albumArt: ImageView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desktop_controller)

        initializeViews()
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        } catch (e: Exception) {
            Log.e(TAG, "获取 MediaSessionManager 失败", e)
            statusText.text = getString(R.string.desktop_status_init_failed, e.message.orEmpty())
        }
        connectButton.setOnClickListener { connectToMediaSession() }

        // Set up control buttons (initially disabled)
        setupControlButtons()
    }

    private fun initializeViews() {
        albumArt = findViewById(R.id.desktop_album_art)
        songTitle = findViewById(R.id.desktop_song_title)
        songArtist = findViewById(R.id.desktop_song_artist)
        prevButton = findViewById(R.id.desktop_prev_button)
        playPauseButton = findViewById(R.id.desktop_play_pause_button)
        nextButton = findViewById(R.id.desktop_next_button)
        statusText = findViewById(R.id.desktop_status_text)
        connectButton = findViewById(R.id.connect_button)

        // Set initial state
        setControlButtonsEnabled(false)
        statusText.text = getString(R.string.desktop_status_disconnected)
    }

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

    private fun setupMediaControllerCallback() {
        val callback = MediaControllerCallback()
        mediaControllerCallback = callback
        mediaController?.registerCallback(callback)
    }

    private fun updateMediaInfo() {
        mediaController?.let { controller ->
            // 获取媒体元数据
            val metadata = controller.metadata
            if (metadata != null) {
                songTitle.text = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: getString(R.string.desktop_default_title)
                songArtist.text = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: getString(R.string.desktop_default_artist)
            }

            // 更新播放状态
            updatePlaybackState()
        }
    }

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

    private fun isPlaying(): Boolean {
        val playbackState = mediaController?.playbackState
        return playbackState?.state == PlaybackStateCompat.STATE_PLAYING
    }

    private fun setControlButtonsEnabled(enabled: Boolean) {
        prevButton.isEnabled = enabled
        playPauseButton.isEnabled = enabled
        nextButton.isEnabled = enabled

        prevButton.alpha = if (enabled) 1.0f else 0.5f
        playPauseButton.alpha = if (enabled) 1.0f else 0.5f
        nextButton.alpha = if (enabled) 1.0f else 0.5f
    }

    private inner class MediaControllerCallback : MediaController.Callback() {

    }

    override fun onDestroy() {
        super.onDestroy()
        mediaControllerCallback?.let { mediaController?.unregisterCallback(it) }
        mediaController = null
    }

    companion object {
        private const val TAG = "DesktopControllerActivity"
    }

}
