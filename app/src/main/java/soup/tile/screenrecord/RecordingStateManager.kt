package soup.tile.screenrecord

import java.lang.ref.WeakReference

object RecordingStateManager {

    private var isRecording: Boolean = false

    private val listeners = ArrayList<WeakReference<OnRecordStateListener>>(2)

    fun isRecording(): Boolean {
        return isRecording
    }

    @JvmStatic
    fun setRecording(isRecording: Boolean) {
        val wasRecording = this.isRecording
        if (wasRecording != isRecording) {
            this.isRecording = isRecording
            dispatchRecordStateChanged(isRecording)
        }
    }

    fun addListener(l: OnRecordStateListener) {
        synchronized(listeners) {
            cleanUpListenersLocked(l)
            listeners.add(WeakReference(l))
        }
    }

    fun removeListener(l: OnRecordStateListener?) {
        synchronized(listeners) {
            cleanUpListenersLocked(l)
        }
    }

    private fun dispatchRecordStateChanged(isRecording: Boolean) {
        synchronized (listeners) {
            var cleanup = false
            listeners.forEach {
                val l = it.get()
                if (l != null) {
                    l.onRecordStateChanged(isRecording)
                } else {
                    cleanup = true
                }
            }
            if (cleanup) {
                cleanUpListenersLocked(null)
            }
        }
    }

    private fun cleanUpListenersLocked(listener: OnRecordStateListener?) {
        listeners.removeIf {
            val found = it.get()
            found == null || found === listener
        }
    }
}
