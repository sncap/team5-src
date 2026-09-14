package com.bbobbo.pet.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bbobbo.pet.MainActivity
import com.bbobbo.pet.R
import com.bbobbo.pet.domain.NotifyKind

/**
 * 기획서 §1-6 알림. 채널은 하나만 쓰고 종류별 ON/OFF 는 설정(DataStore)에서 거른다.
 * POST_NOTIFICATIONS 권한이 없으면 조용히 건너뛴다 — 알림은 게임 진행의 필수 요소가 아니다.
 */
object Notifier {

    private const val CHANNEL_ID = "bbobbo_care"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "뽀뽀 돌보기", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "배고픔·청결·에너지·출석 알림"
            }
        )
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // 바로 위에서 hasPermission() 으로 검사하고, 런타임 회수까지 runCatching 으로 막고 있어
    // lint 의 MissingPermission 경고는 여기서만 억제한다.
    @SuppressLint("MissingPermission")
    fun notify(context: Context, kind: NotifyKind, text: String) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("뽀뽀 키우기")
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 권한이 런타임에 회수된 경우를 대비해 방어한다.
        runCatching {
            NotificationManagerCompat.from(context).notify(kind.ordinal, notification)
        }
    }
}
