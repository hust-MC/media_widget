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

/**
 * 车机桌面主界面。
 * 模拟车机桌面布局，展示音乐播放器控件，通过 MediaSession 连接音乐 App 并控制播放。
 * 负责申请 MEDIA_CONTENT_CONTROL 权限、定期检查连接状态、刷新歌曲信息与播放/暂停操作。
 *
 * 实现 [MediaSessionHelper.ConnectionListener] 以接收连接状态回调，在 UI 上及时反馈给用户。
 */
class MainActivity : AppCompatActivity(), MediaSessionHelper.ConnectionListener {

    /** 专辑封面 ImageView */
    private lateinit var albumArt: ImageView
    /** 歌曲标题 */
    private lateinit var songTitle: TextView
    /** 歌手名 */
    private lateinit var songArtist: TextView
    /** 播放/暂停按钮 */
    private lateinit var playPauseButton: ImageButton
    /** 底部状态文案（如“正在播放”/“已暂停”） */
    private lateinit var statusText: TextView

    /** MediaSession 封装，用于连接音乐 App 并控制播放 */
    private lateinit var mediaSessionHelper: MediaSessionHelper
    /** 主线程 Handler，用于定时任务 */
    private val handler = Handler(Looper.getMainLooper())
    /** 定时任务：检查连接并刷新 UI，每 2 秒执行一次 */
    private val checkConnectionRunnable = object : Runnable {
        override fun run() {
            try {
                if (!mediaSessionHelper.isConnected()) {
                    mediaSessionHelper.tryReconnect()
                }
                updateUI()
            } catch (e: Exception) {
                Log.e(TAG, "定期检查连接/更新UI异常: ${e.message}", e)
            }
            handler.postDelayed(this, 2000) // 每 2 秒再次调度
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        /** 权限请求码，用于 onRequestPermissionsResult 区分 */
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    /**
     * Activity 创建时初始化视图、MediaSession、权限与点击事件。
     *
     * @param savedInstanceState 若由系统重建，则非 null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 绑定布局中的视图
        albumArt = findViewById(R.id.album_art)
        songTitle = findViewById(R.id.song_title)
        songArtist = findViewById(R.id.song_artist)
        playPauseButton = findViewById(R.id.play_pause_button)
        statusText = findViewById(R.id.status_text)

        mediaSessionHelper = MediaSessionHelper(this)
        mediaSessionHelper.setConnectionListener(this)

        // 无权限则弹窗申请，有权限则直接初始化并连接
        checkAndRequestPermissions()

        updateUI()
        // 启动定时检查连接与 UI 刷新
        handler.post(checkConnectionRunnable)

        // 播放按钮：已连接则播放/暂停，未连接则尝试启动音乐 App
        playPauseButton.setOnClickListener {
            if (mediaSessionHelper.isConnected()) {
                if (mediaSessionHelper.isPlaying()) {
                    mediaSessionHelper.pause()
                } else {
                    mediaSessionHelper.play()
                }
            } else {
                launchMusicApp()
            }
        }
    }

    /**
     * 根据当前 MediaSession 状态刷新歌曲标题、歌手、专辑图、播放按钮图标和状态文案。
     */
    private fun updateUI() {
        val currentSong = mediaSessionHelper.getCurrentSong()
        songTitle.text = currentSong.title
        songArtist.text = currentSong.artist

        updateAlbumArt()

        // 按播放状态切换播放/暂停图标并设为白色
        playPauseButton.setImageResource(
            if (mediaSessionHelper.isPlaying()) R.drawable.ic_pause_two_bars else R.drawable.ic_play_arrow
        )
        playPauseButton.setColorFilter(android.graphics.Color.WHITE)

        statusText.text = if (mediaSessionHelper.isPlaying()) getString(R.string.main_status_playing) else getString(R.string.main_status_paused)
    }

    /**
     * 从 MediaSession 获取专辑图并设置到 [albumArt]；无图时使用系统默认播放图标。
     */
    private fun updateAlbumArt() {
        val albumArtBitmap = mediaSessionHelper.getAlbumArt()
        if (albumArtBitmap != null) {
            albumArt.setImageBitmap(albumArtBitmap)
        } else {
            albumArt.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    /**
     * 从后台返回时刷新一次 UI，保证显示与真实播放状态一致。
     */
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    // -------- ConnectionListener 回调 --------

    override fun onConnected() {
        runOnUiThread {
            statusText.text = getString(R.string.main_status_connected)
            updateUI()
        }
    }

    override fun onConnectionFailed(message: String) {
        runOnUiThread {
            statusText.text = getString(R.string.main_status_disconnected)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            statusText.text = getString(R.string.main_status_connection_lost)
            Toast.makeText(this, getString(R.string.toast_connection_lost), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查是否已有 MEDIA_CONTENT_CONTROL 权限：无则请求，有则初始化 MediaSession 并连接。
     */
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

    /**
     * 权限请求结果回调。仅处理 [PERMISSION_REQUEST_CODE]；授予则初始化，拒绝则仍尝试初始化并捕获 SecurityException。
     *
     * @param requestCode  请求码，此处为 [PERMISSION_REQUEST_CODE]
     * @param permissions  请求的权限列表
     * @param grantResults 对应权限的结果（PERMISSION_GRANTED 等）
     */
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
                    Toast.makeText(this, getString(R.string.toast_permission_granted_connecting), Toast.LENGTH_SHORT).show()
                    mediaSessionHelper.initialize()
                } else {
                    Log.e(TAG, "权限被拒绝，尝试直接连接...")
                    Toast.makeText(this, getString(R.string.toast_permission_denied_try), Toast.LENGTH_SHORT).show()
                    // 部分设备拒绝后仍可连接，尝试一次
                    try {
                        mediaSessionHelper.initialize()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "直接连接也失败: ${e.message}")
                        Toast.makeText(this, getString(R.string.toast_need_media_permission), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * 通过包名启动音乐 App（com.max.media_center）。未安装或启动失败时 Toast 提示。
     */
    private fun launchMusicApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.max.media_center")
            if (intent != null) {
                startActivity(intent)
                Toast.makeText(this, getString(R.string.toast_launch_music), Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "未找到音乐播放器 com.max.media_center")
                Toast.makeText(this, getString(R.string.toast_music_app_not_found), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动音乐App失败", e)
            Toast.makeText(this, getString(R.string.toast_launch_failed, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 销毁时取消定时任务、清除监听器并释放 MediaSession 连接与回调。
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkConnectionRunnable)
        mediaSessionHelper.setConnectionListener(null)
        mediaSessionHelper.release()
    }
}
