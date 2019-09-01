package soup.tile.screenrecord

import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

object BuildType {

    fun init(context: Context) {
        Timber.plant(ReleaseTree())
    }

    private class ReleaseTree : Timber.Tree() {

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return
            }
            when {
                t != null -> Crashlytics.logException(t)
                tag != null -> Crashlytics.log("$tag: $message")
                else -> Crashlytics.log(message)
            }
        }
    }
}
