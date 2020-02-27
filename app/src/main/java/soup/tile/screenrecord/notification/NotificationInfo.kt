package soup.tile.screenrecord.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import soup.tile.screenrecord.R

object NotificationInfo {

    const val CHANNEL_ID = "SCR_REC"
    const val NOTIFICATION_ID = 1

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val screenRecord = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.screenrecord_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService<NotificationManager>()
                ?.createNotificationChannels(listOf(screenRecord))
        }
    }
}
