package soup.tile.screenrecord

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Space
import soup.tile.screenrecord.setting.SettingActivity
import soup.tile.screenrecord.util.startForegroundServiceCompat
import soup.tile.screenrecord.util.toast

class ScreenRecordActivity : Activity() {

    private var useAudio = false
    private var fromSettings = false
    private var returnToSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Space(this))
        useAudio = intent?.getBooleanExtra(EXTRA_USE_AUDIO, useAudio) ?: useAudio
        fromSettings = intent?.getBooleanExtra(EXTRA_FROM_SETTINGS, fromSettings) ?: fromSettings
        returnToSettings = intent?.getBooleanExtra(EXTRA_RETURN_TO_SETTINGS, returnToSettings) ?: returnToSettings

        if (useAudio) {
            val permissions = arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO)
            requestScreenCaptureAfterGranted(permissions, fallback= {
                requestPermissions(permissions,
                    REQUEST_CODE_PERMISSIONS_AUDIO
                )
            })
        } else {
            val permissions = arrayOf(WRITE_EXTERNAL_STORAGE)
            requestScreenCaptureAfterGranted(permissions, fallback= {
                requestPermissions(permissions,
                    REQUEST_CODE_PERMISSIONS
                )
            })
        }
    }

    private inline fun requestScreenCaptureAfterGranted(
        permissions: Array<String>,
        fallback: () -> Unit
    ) {
        if (allGranted(permissions)) {
            requestScreenCapture()
        } else {
            fallback()
        }
    }

    private fun Context.allGranted(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS_AUDIO -> {
                requestScreenCaptureAfterGranted(permissions, fallback= {
                    toast(R.string.screenrecord_permission_not_granted)
                    navigateUp()
                })
            }
        }
    }

    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        if (useAudio) {
            startActivityForResult(permissionIntent, REQUEST_CODE_VIDEO_AUDIO)
        } else {
            startActivityForResult(permissionIntent, REQUEST_CODE_VIDEO_ONLY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_VIDEO_ONLY,
            REQUEST_CODE_VIDEO_AUDIO -> {
                if (resultCode == RESULT_OK) {
                    val useAudio = requestCode == REQUEST_CODE_VIDEO_AUDIO
                    val intent = RecordingService.getStartIntent(this, resultCode, data, useAudio, returnToSettings)
                    startForegroundServiceCompat(intent)
                    finish()
                } else {
                    toast(R.string.screenrecord_permission_not_granted)
                    navigateUp()
                }
            }
        }
    }

    private fun navigateUp() {
        if (fromSettings) {
            startActivity(Intent(this, SettingActivity::class.java))
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE_VIDEO_ONLY = 200
        private const val REQUEST_CODE_PERMISSIONS = 299
        private const val REQUEST_CODE_VIDEO_AUDIO = 300
        private const val REQUEST_CODE_PERMISSIONS_AUDIO = 399

        private const val EXTRA_USE_AUDIO = "extra_useAudio"
        private const val EXTRA_FROM_SETTINGS = "extra_fromSettings"
        private const val EXTRA_RETURN_TO_SETTINGS = "extra_returnToSettings"

        fun getStartIntent(
            context: Context,
            useAudio: Boolean,
            fromSettings: Boolean,
            returnToSettings: Boolean
        ): Intent {
            return Intent(context, ScreenRecordActivity::class.java)
                .putExtra(EXTRA_USE_AUDIO, useAudio)
                .putExtra(EXTRA_FROM_SETTINGS, fromSettings)
                .putExtra(EXTRA_RETURN_TO_SETTINGS, returnToSettings)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
