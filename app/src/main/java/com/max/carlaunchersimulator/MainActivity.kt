package com.max.carlaunchersimulator

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var albumArt: ImageView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var statusText: TextView

    private lateinit var mediaSessionHelper: MediaSessionHelper
    private val handler = Handler(Looper.getMainLooper())
    private val checkConnectionRunnable = object : Runnable {
        override fun run() {
            if (!mediaSessionHelper.isConnected()) {
                mediaSessionHelper.tryReconnect()
            }
            updateUI()
            handler.postDelayed(this, 2000) // 每2秒检查一次
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI views
        albumArt = findViewById(R.id.album_art)
        songTitle = findViewById(R.id.song_title)
        songArtist = findViewById(R.id.song_artist)
        playPauseButton = findViewById(R.id.play_pause_button)
        statusText = findViewById(R.id.status_text)

        // Initialize MediaSession
        mediaSessionHelper = MediaSessionHelper(this)
        mediaSessionHelper.initialize()

        // Update UI with current song
        updateUI()
        
        // 开始定期检查连接
        handler.post(checkConnectionRunnable)

        // Set click listeners
        playPauseButton.setOnClickListener {
            if (mediaSessionHelper.isPlaying()) {
                mediaSessionHelper.pause()
            } else {
                mediaSessionHelper.play()
            }
        }
        
        // 点击整个音乐控件区域启动音乐播放App（仅在未连接时）
        findViewById<LinearLayout>(R.id.music_player_widget).setOnClickListener {
            if (mediaSessionHelper.isConnected()) {
                // 如果已连接，直接播放/暂停
                if (mediaSessionHelper.isPlaying()) {
                    mediaSessionHelper.pause()
                } else {
                    mediaSessionHelper.play()
                }
            } else {
                // 如果未连接，启动音乐App
                launchMusicApp()
            }
        }
    }

    private fun updateUI() {
        val currentSong = mediaSessionHelper.getCurrentSong()
        songTitle.text = currentSong.title
        songArtist.text = currentSong.artist

        // Update play/pause button icon
        playPauseButton.setImageResource(
            if (mediaSessionHelper.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )

        // Update status
        statusText.text = if (mediaSessionHelper.isPlaying()) "正在播放 - MediaSession" else "已暂停"
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun launchMusicApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.max.media_center")
            if (intent != null) {
                startActivity(intent)
                Toast.makeText(this, "启动音乐播放器", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未找到音乐播放器App", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkConnectionRunnable)
        mediaSessionHelper.release()
    }
}

