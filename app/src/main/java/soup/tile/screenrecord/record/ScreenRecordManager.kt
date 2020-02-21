package soup.tile.screenrecord.record

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.view.Surface
import timber.log.Timber
import java.io.File
import java.io.IOException

object ScreenRecordManager {

    private const val VIDEO_BIT_RATE = 25000000
    private const val VIDEO_FRAME_RATE = 60

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var inputSurface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var listener: Listener? = null

    interface Listener {

        fun onScreenRecordStateChanged(isRecording: Boolean)

        fun onScreenRecordFileSaved(output: File)
    }

    private val projectionCallback = object : MediaProjection.Callback() {

        override fun onStop() {
            stopRecording()
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun record(mediaProjection: MediaProjection, screenInfo: ScreenInfo) {
        this.mediaProjection = mediaProjection.apply {
            registerCallback(projectionCallback, null)
        }
        startRecording(screenInfo)
    }

    fun stop() {
        stopRecording()
    }

    fun isRecording(): Boolean {
        return mediaRecorder != null
    }

    private lateinit var tempFile: File

    private fun startRecording(screenInfo: ScreenInfo) {
        try {
            tempFile = File.createTempFile("temp", ".mp4")
            Timber.d("Writing video output to: " + tempFile.absolutePath)
            // Set up media recorder
            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // Set up video
                val screenWidth = screenInfo.width
                val screenHeight = screenInfo.height
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(screenWidth, screenHeight)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoEncodingBitRate(VIDEO_BIT_RATE)
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
                    screenInfo.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    inputSurface,
                    null,
                    null
                )
                start()
            }
            listener?.onScreenRecordStateChanged(true)
        } catch (e: IOException) {
            Timber.e(e, "Error starting screen recording: " + e.message)
            listener?.onScreenRecordStateChanged(false)
        }
    }

    private fun stopRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null

        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        inputSurface?.release()
        virtualDisplay?.release()

        listener?.run {
            onScreenRecordStateChanged(false)
            onScreenRecordFileSaved(tempFile)
        }
    }
}
