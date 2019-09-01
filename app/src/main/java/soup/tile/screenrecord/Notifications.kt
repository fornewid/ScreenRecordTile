package soup.tile.screenrecord

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Notifications(private val context: Context) {

    private val nm: NotificationManager = context.getSystemService()!!

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.createChannels()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Context.createChannels() {
        val screenRecord = NotificationChannel(
            SCREEN_RECORD,
            getString(R.string.notification_channel_screen_record),
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannels(listOf(screenRecord))
    }

    fun showSaveState(uri: Uri) {
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val deleteAction = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, DeleteScreenRecordReceiver::class.java)
                .putExtra(SCREEN_RECORD_URI_ID, uri.toString()),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )
        val deleteActionBuilder = NotificationCompat.Action.Builder(
            R.drawable.ic_noti_delete,
            context.getString(R.string.notification_action_delete),
            deleteAction
        )

        val now = System.currentTimeMillis()
        val notification = NotificationCompat
            .Builder(context, SCREEN_RECORD)
            .setContentTitle(context.getString(R.string.screen_record_saved_title))
            .setContentText(context.getString(R.string.screen_record_saved_text))
            .setContentIntent(PendingIntent.getActivity(context, 0, launchIntent, 0))
            .setSmallIcon(R.drawable.ic_noti_video)
            .setWhen(now)
            .setShowWhen(true)
            .setAutoCancel(true)
            .addAction(deleteActionBuilder.build())
            .build()
        nm.notify(ID_SCREEN_RECORD, notification)
    }

    class DeleteScreenRecordReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra(SCREEN_RECORD_URI_ID).not()) {
                return
            }

            context.getSystemService<NotificationManager>()?.cancel(ID_SCREEN_RECORD)
            val uri = Uri.parse(intent.getStringExtra(SCREEN_RECORD_URI_ID))
            GlobalScope.launch(Dispatchers.IO) {
                context.deleteImageInBackground(uri)
            }
        }

        private fun Context.deleteImageInBackground(uri: Uri) {
            contentResolver.delete(uri, null, null)
        }
    }

    companion object {

        private const val SCREEN_RECORD_URI_ID = "screen_record_uri_id"
        private const val SCREEN_RECORD = "SCR_REC"
        private const val ID_SCREEN_RECORD = 1
    }
}
