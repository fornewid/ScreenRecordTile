package soup.tile.screenrecord.record

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaFormat.*
import android.media.projection.MediaProjection
import android.os.Looper
import androidx.annotation.WorkerThread
import soup.tile.screenrecord.storage.FileData
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.min

class VideoEncoder(
    mediaProjection: MediaProjection,
    private val output: FileData,
    private val callback: Callback
) {

    data class Config(
        val width: Int,
        val height: Int,
        val bitRate: Int,
        val frameRate: Int,
        val iFrameInterval: Int,
        val mimeType: String
    )

    interface Callback {

        fun onOutputFormatChanged(format: MediaFormat) {}

        fun onOutputBufferAvailable(index: Int, info: MediaCodec.BufferInfo) {}

        fun onError(exception: Exception) {}
    }

    private val config: Config = getConfig()
    private val display: VirtualDisplay =
        mediaProjection.createVirtualDisplay(config.width, config.height)
    private val mediaCodec: MediaCodec by lazy { createMediaCodec(config.toMediaFormat()) }

    @WorkerThread
    @Throws(IOException::class)
    fun prepare() {
        if (Looper.myLooper() == null || Looper.myLooper() == Looper.getMainLooper()) {
            throw IllegalStateException("should getEncoderConfigAsync in a HandlerThread")
        }
        display.surface = mediaCodec.createInputSurface()
        Timber.i("VideoEncoder create input surface: $display.surface")
        mediaCodec.start()
    }

    fun consumeOutputBuffer(index: Int, consume: (ByteBuffer?) -> Unit) {
        consume(mediaCodec.getOutputBuffer(index))
        mediaCodec.releaseOutputBuffer(index, false)
    }

    fun stop() {
        mediaCodec.stop()
    }

    fun release() {
        display.surface?.release()
        display.surface = null
        display.release()
        mediaCodec.release()
    }

    /* private functions */

    private fun MediaProjection.createVirtualDisplay(width: Int, height: Int): VirtualDisplay {
        return createVirtualDisplay(
            "ScreenRecord",
            width, height, 1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, null, null, null
        )
    }

    private fun Config.toMediaFormat(): MediaFormat {
        return createVideoFormat(mimeType, width, height).apply {
            setInteger(KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(KEY_BIT_RATE, bitRate)
            setInteger(KEY_FRAME_RATE, frameRate)
            setInteger(KEY_I_FRAME_INTERVAL, iFrameInterval)
        }
    }

    private fun createMediaCodec(format: MediaFormat): MediaCodec {
        Timber.d("Create mediaStorage codecFormat: $format")
        return MediaCodec.createEncoderByType(format.getString(KEY_MIME)).apply {
            setCallback(object : MediaCodec.Callback() {

                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    callback.onOutputBufferAvailable(index, info)
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    callback.onOutputFormatChanged(format)
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    callback.onError(e)
                }
            })
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    private fun getConfig(): Config {
        val mimeType = output.mimeType
        return mediaCodecInfoList()
            .mapNotNull { it.videoCodecOrNull(mimeType) }
            .mapNotNull { it.videoEncoderConfig(mimeType) }
            .first()
    }

    private fun mediaCodecInfoList(): List<MediaCodecInfo> {
        return MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.toList()
    }

    private fun MediaCodecInfo.videoCodecOrNull(mimeType: String): MediaCodecInfo.VideoCapabilities? {
        return try {
            getCapabilitiesForType(mimeType).videoCapabilities
        } catch (e: Exception) {
            null
        }
    }

    private fun MediaCodecInfo.VideoCapabilities.videoEncoderConfig(mimeType: String): Config? {
        val frameRate = min(60, supportedFrameRates.upper)
        val bitrate = 1000 * min(25000, bitrateRange.upper)
        return output.size
            .takeIf {
                areSizeAndRateSupported(it.width, it.height, frameRate.toDouble())
            }?.let {
                Config(
                    it.width,
                    it.height,
                    bitrate,
                    frameRate,
                    1,
                    mimeType
                )
            }
    }
}
