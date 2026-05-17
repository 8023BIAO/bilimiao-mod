package com.a10miaomiao.bilimiao.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.a10miaomiao.bilimiao.MainActivity
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger

class PlaybackService : Service() {

    companion object {
        var instance: PlaybackService? = null
            private set
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "bilimiao_playback"
        const val CHANNEL_NAME = "播放控制"

        const val ACTION_PLAY_PAUSE = "com.a10miaomiao.bilimiao.PLAY_PAUSE"
        const val ACTION_SKIP_BACK = "com.a10miaomiao.bilimiao.SKIP_BACK"
        const val ACTION_SKIP_FORWARD = "com.a10miaomiao.bilimiao.SKIP_FORWARD"
        const val ACTION_CLOSE = "com.a10miaomiao.bilimiao.CLOSE"
    }

    private var playerDelegate: BasePlayerDelegate? = null
    private var isPlaying = false
    private var notificationTitle: String? = null
    private var notificationSubtitle: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Android 15+ 必须先创建 NotificationChannel 再调 startForeground
        createNotificationChannel()
        startForegroundCompat(buildPlaceholderNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 15+ 要求 startForegroundService 后 5 秒内必须调 startForeground。
        // 如果service已创建但不在前台（unbindPlayer 调了 stopForeground），
        // 再次 startForegroundService 只会触发 onStartCommand 不会触发 onCreate，
        // 所以必须在这里也确保 startForeground 被调用。
        startForegroundCompat(buildPlaceholderNotification())

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (isPlaying) {
                    playerDelegate?.mediaPause()
                    isPlaying = false
                } else {
                    playerDelegate?.mediaPlay()
                    isPlaying = true
                }
                rebuildNotification()
            }
            ACTION_SKIP_BACK -> playerDelegate?.mediaSeekBack()
            ACTION_SKIP_FORWARD -> playerDelegate?.mediaSeekForward()
            ACTION_CLOSE -> {
                playerDelegate?.closePlayer()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun setPlayerDelegate(delegate: BasePlayerDelegate) {
        playerDelegate = delegate
    }

    /** 显示通知栏（视频打开时调用） */
    fun bindPlayer() {
        if (notificationTitle.isNullOrEmpty()) return
        isPlaying = false
        val notification = buildNotification()
        startForegroundCompat(notification)
    }

    /** 隐藏通知栏（视频关闭时调用） */
    fun unbindPlayer() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationTitle = null
        notificationSubtitle = null
        isPlaying = false
    }

    private fun rebuildNotification() {
        if (notificationTitle.isNullOrEmpty()) return
        val notification = buildNotification()
        startForegroundCompat(notification)
    }

    /** 播放状态变化时更新通知栏图标 */
    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        rebuildNotification()
    }

    fun updateMetadata(title: String?, subtitle: String?, coverUrl: String?) {
        notificationTitle = title ?: "bilimiao"
        notificationSubtitle = subtitle
    }

    fun clearMetadata() {
        notificationTitle = null
        notificationSubtitle = null
        isPlaying = false
    }

    private fun buildPlaceholderNotification(): android.app.Notification {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPI = PendingIntent.getActivity(this, 4, contentIntent, flag)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("bilimiao")
            .setContentText("播放服务就绪")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPI)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "视频播放控制"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // 中间：播放/暂停
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause
        else android.R.drawable.ic_media_play
        val playPauseLabel = if (isPlaying) "暂停" else "播放"
        val ppIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val ppPI = PendingIntent.getService(this, 0, ppIntent, flag)

        // 左边：后退10秒
        val sbIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_SKIP_BACK }
        val sbPI = PendingIntent.getService(this, 1, sbIntent, flag)

        // 右边：前进10秒
        val sfIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_SKIP_FORWARD }
        val sfPI = PendingIntent.getService(this, 2, sfIntent, flag)

        // 关闭
        val closeIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_CLOSE }
        val closePI = PendingIntent.getService(this, 3, closeIntent, flag)

        // 点击通知进入APP
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPI = PendingIntent.getActivity(this, 4, contentIntent, flag)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle ?: "bilimiao")
            .setContentText(notificationSubtitle ?: "正在播放")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPI)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_rew, "后退10s", sbPI)
            .addAction(playPauseIcon, playPauseLabel, ppPI)
            .addAction(android.R.drawable.ic_media_ff, "前进10s", sfPI)
            .addAction(android.R.drawable.ic_delete, "关闭", closePI)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setCancelButtonIntent(closePI))
            .build()
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            miaoLogger().e("startForeground failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        instance = null
        playerDelegate = null
        super.onDestroy()
    }
}
