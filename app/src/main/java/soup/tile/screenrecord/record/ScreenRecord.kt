package soup.tile.screenrecord.record

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import soup.tile.screenrecord.storage.FileData
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenRecord(
    mediaProjection: MediaProjection,
    private val output: FileData,
    private val callback: Callback
) {

    interface Callback {

        fun onStart()

        fun onStop()

        fun onError(throwable: Throwable?)

        fun onRecording(presentationTimeUs: Long)
    }

    private val videoEncoder = VideoEncoder(
        mediaProjection,
        output,
        object : VideoEncoder.Callback {

            override fun onOutputBufferAvailable(index: Int, info: MediaCodec.BufferInfo) {
                Timber.i("VideoEncoder output buffer available: index=$index")
                try {
                    muxVideo(index, info)
                } catch (e: Exception) {
                    Timber.e(e, "Muxer encountered an error! ")
                    bgHandler.post {
                        onErrorInternal(e)
                    }
                }
            }

            override fun onOutputFormatChanged(format: MediaFormat) {
                startMuxerIfReady(format)
            }

            override fun onError(exception: Exception) {
                Timber.e(exception, "VideoEncoder ran into an error!")
                bgHandler.post {
                    onErrorInternal(exception)
                }
            }
        })

    private val worker = HandlerThread("ScreenRecord").apply { start() }
    private val bgHandler = Handler(worker.looper)
    private val uiHandler = Handler()

    private val isForceQuit = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private lateinit var muxer: MediaMuxer

    private var videoTrackIndex = INVALID_INDEX
    private var videoPtsOffset: Long = 0

    private val pendingVideoEncoderBuffer = ArrayList<Pair<Int, MediaCodec.BufferInfo>>()

    fun start() {
        bgHandler.post {
            onStartInternal()
        }
    }

    fun stop() {
        isForceQuit.set(true)
        if (!isRunning.get()) {
            release()
        } else {
            bgHandler.post {
                onStopInternal(withEOS = true)
            }
        }
    }

    fun isRecording(): Boolean {
        return isRunning.get()
    }

    private fun onStartInternal() {
        var exception: Throwable? = null
        try {
            startRecord()
        } catch (e: Exception) {
            exception = e
        }
        if (exception == null) {
            uiHandler.post {
                callback.onStart()
            }
        } else {
            onErrorInternal(exception)
        }
    }

    private fun onStopInternal(withEOS: Boolean) {
        stopEncoders()
        if (withEOS) {
            signalEndOfStream()
        }
        release()

        uiHandler.post {
            callback.onStop()
        }
    }

    private fun onErrorInternal(throwable: Throwable?) {
        stopEncoders()
        signalEndOfStream()
        release()

        uiHandler.post {
            callback.onError(throwable)
        }
    }

    private fun signalEndOfStream() {
        Timber.i("Signal EOS to muxer ")
        if (videoTrackIndex != INVALID_INDEX) {
            val eos = MediaCodec.BufferInfo().apply {
                set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            val buffer = ByteBuffer.allocate(0)
            writeSampleData(videoTrackIndex, eos, buffer)
        }
        videoTrackIndex = INVALID_INDEX
    }

    @Throws(Throwable::class)
    private fun startRecord() {
        if (isRunning.get() || isForceQuit.get()) {
            throw IllegalStateException()
        }
        isRunning.set(true)

        try {
            videoEncoder.prepare()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun muxVideo(index: Int, buffer: MediaCodec.BufferInfo) {
        if (!isRunning.get()) {
            Timber.w("muxVideo: Already stopped!")
            return
        }
        if (::muxer.isInitialized.not() || videoTrackIndex == INVALID_INDEX) {
            pendingVideoEncoderBuffer.add(index to buffer)
            return
        }
        videoEncoder.consumeOutputBuffer(index) {
            writeSampleData(videoTrackIndex, buffer, it)
        }
        if (buffer.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            Timber.d("Stop encoder and muxer, since the buffer has been marked with EOS")
            // send release msg
            videoTrackIndex = INVALID_INDEX
            bgHandler.postAtFrontOfQueue {
                onStopInternal(withEOS = false)
            }
        }
    }

    private fun writeSampleData(
        track: Int,
        buffer: MediaCodec.BufferInfo,
        encodedData: ByteBuffer?
    ) {
        if (buffer.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            Timber.d("Ignoring BUFFER_FLAG_CODEC_CONFIG")
            buffer.size = 0
        }
        val eos = buffer.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
        if (buffer.size == 0 && !eos) {
            Timber.d("info.size == 0, drop it.")
            return
        }
        if (buffer.presentationTimeUs != 0L) { // maybe 0 if eos
            if (track == videoTrackIndex) {
                if (videoPtsOffset == 0L) {
                    videoPtsOffset = buffer.presentationTimeUs
                    buffer.presentationTimeUs = 0
                } else {
                    buffer.presentationTimeUs -= videoPtsOffset
                }
            }
        }
        Timber.d("[${Thread.currentThread().id}] Got buffer, track=$track, info: size=${buffer.size}, presentationTimeUs=${buffer.presentationTimeUs}")
        if (!eos) {
            uiHandler.post {
                callback.onRecording(buffer.presentationTimeUs)
            }
        }
        if (encodedData != null) {
            encodedData.position(buffer.offset)
            encodedData.limit(buffer.offset + buffer.size)
            muxer.writeSampleData(track, encodedData, buffer)
            Timber.i("Sent ${buffer.size} bytes to MediaMuxer on track $track")
        }
    }

    private fun startMuxerIfReady(newFormat: MediaFormat) {
        // should happen before receiving buffers, and should only happen once
        if (videoTrackIndex >= 0 || ::muxer.isInitialized) {
            throw IllegalStateException("output format already changed!")
        }
        Timber.i("Video output format changed. New format: $newFormat")

        muxer = MediaMuxer(output.file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        videoTrackIndex = muxer.addTrack(newFormat)
        muxer.start()
        Timber.i("Started mediaStorage muxer, videoIndex=$videoTrackIndex")
        if (pendingVideoEncoderBuffer.isEmpty()) {
            return
        }
        Timber.i("Mux pending config output buffers...")
        pendingVideoEncoderBuffer.forEach { (index, bufferInfo) ->
            muxVideo(index, bufferInfo)
        }
        Timber.i("Mux pending config output buffers done.")
    }

    private fun stopEncoders() {
        isRunning.set(false)
        pendingVideoEncoderBuffer.clear()

        try {
            videoEncoder.stop()
        } catch (e: IllegalStateException) {
            // ignored
        }
    }

    private fun release() {
        videoTrackIndex = INVALID_INDEX

        worker.quitSafely()

        videoEncoder.release()

        try {
            muxer.stop()
            muxer.release()
        } catch (e: Exception) {
            // ignored
        }
    }

    companion object {

        private const val INVALID_INDEX = -1
    }
}
