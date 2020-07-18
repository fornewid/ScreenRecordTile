package soup.tile.screenrecord.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import soup.tile.screenrecord.R

class PreferenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val icon: ImageView
    private val title: TextView
    private val summary: TextView

    init {
        View.inflate(context, R.layout.preference, this)
        icon = findViewById(android.R.id.icon)
        title = findViewById(android.R.id.title)
        summary = findViewById(android.R.id.summary)

        val a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceView)

        val iconResId = a.getResourceId(R.styleable.PreferenceView_android_icon, 0)
        if (iconResId != 0) {
            setIcon(iconResId)
        }
        setTitle(a.getString(R.styleable.PreferenceView_android_title))
        setSummary(a.getString(R.styleable.PreferenceView_android_summary))

        a.recycle()
    }

    fun setIcon(resId: Int) {
        icon.setImageResource(resId)
    }

    fun setTitle(text: String?) {
        title.text = text
        title.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    fun setSummary(text: String?) {
        summary.text = text
        summary.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
}
