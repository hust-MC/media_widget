package com.max.carlaunchersimulator

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DesktopControllerActivity : AppCompatActivity() {

    private lateinit var mediaSessionManager: MediaSessionManager
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

        // Initialize UI
        initializeViews()

        // Initialize MediaSession Manager
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        // Set up connect button
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
        statusText.text = "Disconnected from MediaSession"
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
        try {
            // 获取活跃的媒体会话
            val activeSessions = mediaSessionManager.getActiveSessions(
                ComponentName(this, DesktopControllerActivity::class.java)
            )

            if (activeSessions.isNotEmpty()) {
                // 连接到第一个活跃的媒体会话
                val sessionController = activeSessions[0]
                val sessionToken = sessionController.sessionToken
                mediaController = MediaController(this, sessionToken)

                // 设置回调以监听媒体信息变化
                setupMediaControllerCallback()

                // 启用控制按钮
                setControlButtonsEnabled(true)
                statusText.text = "Connected to MediaSession"

                // 更新UI显示当前媒体信息
                updateMediaInfo()

            } else {
                statusText.text = "No active MediaSession found"
            }
        } catch (e: SecurityException) {
            statusText.text = "Permission denied: ${e.message}"
        }
    }

    private fun setupMediaControllerCallback() {
        mediaControllerCallback = MediaControllerCallback()
        mediaController?.registerCallback(mediaControllerCallback!!)
    }

    private fun updateMediaInfo() {
        mediaController?.let { controller ->
            // 获取媒体元数据
            val metadata = controller.metadata
            if (metadata != null) {
                songTitle.text = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
                songArtist.text = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
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
        mediaController?.unregisterCallback(mediaControllerCallback!!)
        mediaController = null
    }
}
