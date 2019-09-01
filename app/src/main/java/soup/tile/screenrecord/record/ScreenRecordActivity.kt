package soup.tile.screenrecord.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.media.MediaFormat
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Size
import android.widget.Space
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import soup.tile.screenrecord.R
import soup.tile.screenrecord.storage.FileData
import soup.tile.screenrecord.storage.FileFactory
import soup.tile.screenrecord.util.toast

class ScreenRecordActivity : Activity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var screenSize: Size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Space(this))
        mediaProjectionManager = getSystemService()!!
        screenSize = getFullscreenSize()

        if (hasPermissions()) {
            requestMediaProjection()
        } else {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS)
        }
    }

    private fun getFullscreenSize(): Size {
        return Point()
            .apply(windowManager.defaultDisplay::getRealSize)
            .run { Size(x, y) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasPermissions()) {
                requestMediaProjection()
            } else {
                toast(R.string.permission_not_exist)
                finish()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestMediaProjection() {
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            data?.let { mediaProjectionManager.getMediaProjection(resultCode, it) }
                ?.let { mediaProjection ->
                    ScreenRecordManager.record(
                        mediaProjection,
                        createNewFile(screenSize)
                    )
                }
            finish()
        }
    }

    private fun Context.createNewFile(screen: Size): FileData {
        val timestamp = System.currentTimeMillis()
        return FileData(
            file = FileFactory.createNewFile(this, timestamp),
            size = screen,
            timestamp = timestamp,
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC // H.264 Advanced Video Coding
        )
    }

    companion object {

        private const val REQUEST_MEDIA_PROJECTION = 1
        private const val REQUEST_PERMISSIONS = 2

        private val PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
