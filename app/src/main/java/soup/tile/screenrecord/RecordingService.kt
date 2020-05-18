package soup.tile.screenrecord

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import soup.tile.screenrecord.RecordingStateManager.setRecording
import soup.tile.screenrecord.notification.NotificationInfo.CHANNEL_ID
import soup.tile.screenrecord.notification.NotificationInfo.NOTIFICATION_ID
import soup.tile.screenrecord.util.FileFactory
import soup.tile.screenrecord.util.toast
import timber.log.Timber
import java.io.File
import java.io.IOException

class RecordingService : Service() {
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private var mediaProjection: MediaProjection? = null
    private var inputSurface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var useAudio = false
    private lateinit var tempFile: File

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_START -> {
                useAudio = intent.getBooleanExtra(EXTRA_USE_AUDIO, false)
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (data != null) {
                    startForeground(NOTIFICATION_ID, createRecordingNotification())
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

                    Handler().postDelayed(::startRecording, 300)
                }
            }
            ACTION_CANCEL -> {
                stopRecording()

                // Delete temp file
                if (tempFile.delete()) {
                    toast(R.string.screenrecord_cancel_success)
                } else {
                    toast(R.string.screenrecord_error)
                }

                // Close quick shade
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
            ACTION_STOP -> {
                stopRecording()
                saveRecording()
            }
            ACTION_SHARE -> {
                // Close quick shade
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

                // Remove notification
                notificationManager.cancel(NOTIFICATION_ID)

                val shareIntent = Intent(Intent.ACTION_SEND)
                    .setType("video/mp4")
                    .putExtra(Intent.EXTRA_STREAM, Uri.parse(intent.getStringExtra(EXTRA_PATH)))
                val shareLabel = resources.getString(R.string.screenrecord_share_label)
                Intent.createChooser(shareIntent, shareLabel)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .run(::startActivity)
            }
            ACTION_DELETE -> {
                // Close quick shade
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                val uri = Uri.parse(intent.getStringExtra(EXTRA_PATH))
                contentResolver.delete(uri, null, null)

                toast(R.string.screenrecord_delete_description)

                // Remove notification
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
        return START_STICKY
    }

    /**
     * Begin the recording session
     */
    private fun startRecording() {
        try {
            tempFile = File.createTempFile("temp", ".mp4")
            Timber.d("Writing video output to: ${tempFile.absolutePath}")
            // Set up media recorder
            mediaRecorder = MediaRecorder().apply {
                if (useAudio) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // Set up video
                val screenSize = getScreenSize()
                val screenWidth = screenSize.width
                val screenHeight = screenSize.height
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(screenWidth, screenHeight)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoEncodingBitRate(VIDEO_BIT_RATE)

                // Set up audio
                if (useAudio) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setAudioChannels(TOTAL_NUM_TRACKS)
                    setAudioEncodingBitRate(AUDIO_BIT_RATE)
                    setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setOutputFile(tempFile)
                } else {
                    setOutputFile(tempFile.absolutePath)
                }
                prepare()

                // Create surface
                inputSurface = surface
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "Recording Display",
                    screenWidth,
                    screenHeight,
                    resources.displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    inputSurface,
                    null,
                    null
                )
                start()
            }

            setRecording(true)
        } catch (e: IOException) {
            setRecording(false)
            Timber.e(e, "Error starting screen recording: ${e.message}")
            throw RuntimeException(e)
        }
    }

    private fun createRecordingNotification(): Notification {
        val stopAction = NotificationCompat.Action
            .Builder(
                R.drawable.ic_noti_stop,
                resources.getString(R.string.screenrecord_stop_label),
                PendingIntent.getService(
                    this,
                    REQUEST_CODE,
                    getStopIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        val cancelAction = NotificationCompat.Action
            .Builder(
                R.drawable.ic_noti_cancel,
                resources.getString(R.string.screenrecord_cancel_label),
                PendingIntent.getService(
                    this,
                    REQUEST_CODE,
                    getCancelIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti_video)
            .setContentTitle(resources.getString(R.string.screenrecord_name))
            .setUsesChronometer(true)
            .setOngoing(true)
            .addAction(stopAction)
            .addAction(cancelAction)
            .build()
    }

    private fun createSaveNotification(uri: Uri, file: File): Notification {
        val viewIntent = Intent(Intent.ACTION_VIEW)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(uri, "video/mp4")
        val shareAction = NotificationCompat.Action
            .Builder(
                R.drawable.ic_noti_share,
                resources.getString(R.string.screenrecord_share_label),
                PendingIntent.getService(
                    this,
                    REQUEST_CODE,
                    getShareIntent(this, uri.toString()),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        val deleteAction = NotificationCompat.Action
            .Builder(
                R.drawable.ic_noti_delete,
                resources.getString(R.string.screenrecord_delete_label),
                PendingIntent.getService(
                    this,
                    REQUEST_CODE,
                    getDeleteIntent(this, uri.toString()),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti_video)
            .setContentTitle(resources.getString(R.string.screenrecord_name))
            .setContentText(resources.getString(R.string.screenrecord_save_message))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE,
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(shareAction)
            .addAction(deleteAction)
            .setAutoCancel(true)

        // Add thumbnail if available
        try {
            val thumbnailBitmap = MediaMetadataRetriever().run {
                setDataSource(file.absolutePath)
                ThumbnailUtils.extractThumbnail(getFrameAtTime(1), 512, 384)
            }
            val pictureStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(thumbnailBitmap)
                .bigLargeIcon(null)
            builder.setLargeIcon(thumbnailBitmap).setStyle(pictureStyle)
        } catch (e: Exception) {
            Timber.e(e, "Error creating thumbnail: ${e.message}")
        }

        return builder.build()
    }

    private fun stopRecording() {
        setRecording(false)
        mediaRecorder?.run {
            stop()
            release()
        }
        mediaRecorder = null
        mediaProjection?.stop()
        mediaProjection = null
        inputSurface?.release()
        virtualDisplay?.release()
        stopSelf()
    }

    private fun saveRecording() {
        val timeMillis = System.currentTimeMillis()
        val fileName = FileFactory.fileName(timeMillis)
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.VideoColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Video.VideoColumns.DATE_ADDED, timeMillis)
            put(MediaStore.Video.VideoColumns.MIME_TYPE, "video/mp4")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.VideoColumns.DATE_TAKEN, timeMillis)
            }
        }
        val itemUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (itemUri == null) {
            toast(R.string.screenrecord_error)
            return
        }
        try { // Add to the mediastore
            contentResolver.openOutputStream(itemUri, "w")?.use { os ->
                tempFile.inputStream().copyTo(os)
            }
            notificationManager.notify(NOTIFICATION_ID, createSaveNotification(itemUri, tempFile))
            tempFile.delete()
        } catch (e: IOException) {
            Timber.e(e,"Error saving screen recording: ${e.message}")
            toast(R.string.screenrecord_error)
        }
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "extra_resultCode"
        private const val EXTRA_DATA = "extra_data"
        private const val EXTRA_PATH = "extra_path"
        private const val EXTRA_USE_AUDIO = "extra_useAudio"
        private const val REQUEST_CODE = 2

        private const val ACTION_START = "soup.tile.screenrecord.START"
        private const val ACTION_STOP = "soup.tile.screenrecord.STOP"
        private const val ACTION_CANCEL = "soup.tile.screenrecord.CANCEL"
        private const val ACTION_SHARE = "soup.tile.screenrecord.SHARE"
        private const val ACTION_DELETE = "soup.tile.screenrecord.DELETE"

        private const val TOTAL_NUM_TRACKS = 1
        private const val VIDEO_BIT_RATE = 25000000 //6000000;
        private const val VIDEO_FRAME_RATE = 60 //30;
        private const val AUDIO_BIT_RATE = 16
        private const val AUDIO_SAMPLE_RATE = 44100

        /**
         * Get an intent to start the recording service.
         *
         * @param context    Context from the requesting activity
         * @param resultCode The result code from [Activity.onActivityResult]
         * @param data       The data from [Activity.onActivityResult]
         * @param useAudio   True to enable microphone input while recording
         */
        fun getStartIntent(context: Context, resultCode: Int, data: Intent?, useAudio: Boolean): Intent {
            return Intent(context, RecordingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)
                .putExtra(EXTRA_USE_AUDIO, useAudio)
        }

        fun getStopIntent(context: Context): Intent {
            return Intent(context, RecordingService::class.java)
                .setAction(ACTION_STOP)
        }

        private fun getCancelIntent(context: Context): Intent {
            return Intent(context, RecordingService::class.java)
                .setAction(ACTION_CANCEL)
        }

        private fun getShareIntent(context: Context, path: String): Intent {
            return Intent(context, RecordingService::class.java)
                .setAction(ACTION_SHARE)
                .putExtra(EXTRA_PATH, path)
        }

        private fun getDeleteIntent(context: Context, path: String): Intent {
            return Intent(context, RecordingService::class.java)
                .setAction(ACTION_DELETE)
                .putExtra(EXTRA_PATH, path)
        }

        private fun Context.getScreenSize(): Size {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return Point()
                .apply { windowManager.defaultDisplay.getRealSize(this) }
                .let { Size(it.x, it.y) }
        }
    }
}
