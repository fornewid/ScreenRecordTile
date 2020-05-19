package soup.tile.screenrecord.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import soup.tile.screenrecord.R

class PreferenceCategoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val title: TextView

    init {
        View.inflate(context, R.layout.preference_category, this)
        title = findViewById(android.R.id.title)

        val a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceCategoryView)
        setTitle(a.getString(R.styleable.PreferenceCategoryView_android_title))
        a.recycle()
    }

    fun setTitle(text: String?) {
        title.text = text
    }
}
