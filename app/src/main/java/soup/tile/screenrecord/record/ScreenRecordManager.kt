package soup.tile.screenrecord.record

import android.media.projection.MediaProjection
import soup.tile.screenrecord.storage.FileData

object ScreenRecordManager {

    interface Listener {

        fun onScreenRecordStateChanged(isRecording: Boolean)

        fun onScreenRecordFileSaved(output: FileData)
    }

    private var mediaProjection: MediaProjection? = null
    private var recorder: ScreenRecord? = null

    private var listener: Listener? = null

    private val projectionCallback = object : MediaProjection.Callback() {

        override fun onStop() {
            stopRecorder()
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun record(mediaProjection: MediaProjection, output: FileData) {
        this.mediaProjection = mediaProjection.apply {
            registerCallback(projectionCallback, null)
            recorder = createScreenRecorder(output).apply {
                start()
            }
        }
    }

    fun stop() {
        stopRecorder()
    }

    fun isRecording(): Boolean {
        return recorder != null
    }

    private fun MediaProjection.createScreenRecorder(output: FileData): ScreenRecord {
        return ScreenRecord(this, output, object : ScreenRecord.Callback {

            override fun onStart() {
//                notifications.recording(0)
                listener?.onScreenRecordStateChanged(true)
            }

            override fun onStop() {
                stopRecorder()

                listener?.run {
                    onScreenRecordStateChanged(false)
                    onScreenRecordFileSaved(output)
                }
            }

            override fun onError(throwable: Throwable?) {
                stopRecorder()
                output.file.delete()

                listener?.onScreenRecordStateChanged(false)
            }

            override fun onRecording(presentationTimeUs: Long) {
//                notifications.recording(presentationTimeUs / 1000)
            }
        })
    }

    private fun stopRecorder() {
        if (recorder == null) return
        recorder?.stop()
        recorder = null

        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }
}
