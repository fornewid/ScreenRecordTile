package soup.tile.screenrecord.util

import android.view.View

private typealias OnClickListener = (View) -> Unit

fun View.setOnDebounceClickListener(listener: OnClickListener?) {
    if (listener == null) {
        setOnClickListener(null)
    } else {
        setOnClickListener(OnDebounceClickListener(listener))
    }
}

class OnDebounceClickListener(private val listener: OnClickListener) : View.OnClickListener {

    override fun onClick(v: View?) {
        val now = System.currentTimeMillis()
        if (now - lastTime < INTERVAL) return
        lastTime = now
        v?.run(listener)
    }

    companion object {

        private const val INTERVAL: Long = 300

        private var lastTime: Long = 0
    }
}
