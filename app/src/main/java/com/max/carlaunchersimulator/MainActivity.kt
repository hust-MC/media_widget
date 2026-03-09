package com.max.carlaunchersimulator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
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

        // Initialize MediaSession first
        mediaSessionHelper = MediaSessionHelper(this)
        
        // 检查并申请权限
        checkAndRequestPermissions()

        // Update UI with current song
        updateUI()
        
        // 开始定期检查连接
        handler.post(checkConnectionRunnable)

        // Set click listeners - 只有播放按钮有点击事件
        playPauseButton.setOnClickListener {
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

        // Update album art from MediaSession
        updateAlbumArt()

        // Update play/pause button icon (white color)
        playPauseButton.setImageResource(
            if (mediaSessionHelper.isPlaying()) R.drawable.ic_pause_two_bars else R.drawable.ic_play_arrow
        )
        playPauseButton.setColorFilter(android.graphics.Color.WHITE)

        // Update status
        statusText.text = if (mediaSessionHelper.isPlaying()) "正在播放 - MediaSession" else "已暂停"
    }
    
    private fun updateAlbumArt() {
        val albumArtBitmap = mediaSessionHelper.getAlbumArt()
        if (albumArtBitmap != null) {
            albumArt.setImageBitmap(albumArtBitmap)
        } else {
            // 使用默认图标
            albumArt.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MEDIA_CONTENT_CONTROL) 
            != PERMISSION_GRANTED) {
            Log.d(TAG, "申请MEDIA_CONTENT_CONTROL权限...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.MEDIA_CONTENT_CONTROL),
                PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "权限已授予，初始化MediaSession...")
            mediaSessionHelper.initialize()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    Log.d(TAG, "权限已授予，初始化MediaSession...")
                    Toast.makeText(this, "权限已授予，正在尝试连接...", Toast.LENGTH_SHORT).show()
                    mediaSessionHelper.initialize()
                } else {
                    Log.e(TAG, "权限被拒绝，尝试直接连接...")
                    Toast.makeText(this, "权限被拒绝，尝试直接连接...", Toast.LENGTH_SHORT).show()
                    // 即使权限被拒绝，也尝试直接连接（某些情况下可能仍然有效）
                    try {
                        mediaSessionHelper.initialize()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "直接连接也失败: ${e.message}")
                        Toast.makeText(this, "需要媒体控制权限，请在设置中手动授予", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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

