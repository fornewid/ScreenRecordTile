package soup.tile.screenrecord.preference

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import soup.tile.screenrecord.R

class SwitchPreferenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val pref: SharedPreferences = context.defaultSharedPreference
    private val icon: ImageView
    private val title: TextView
    private val summary: TextView
    private val switch: Switch

    private val key: String

    init {
        View.inflate(context, R.layout.preference_switch, this)
        icon = findViewById(android.R.id.icon)
        title = findViewById(android.R.id.title)
        summary = findViewById(android.R.id.summary)
        switch = findViewById(android.R.id.switch_widget)

        val a = context.obtainStyledAttributes(attrs, R.styleable.SwitchPreferenceView)

        val iconResId = a.getResourceId(R.styleable.SwitchPreferenceView_android_icon, 0)
        if (iconResId != 0) {
            setIcon(iconResId)
        }
        setTitle(a.getString(R.styleable.SwitchPreferenceView_android_title))
        setSummary(a.getString(R.styleable.SwitchPreferenceView_android_summary))

        val maybeKey = a.getString(R.styleable.SwitchPreferenceView_android_key)
        checkNotNull(maybeKey) { "Preference does not have a key assigned." }
        key = maybeKey

        val defaultValue = a.getBoolean(R.styleable.SwitchPreferenceView_android_defaultValue, false)
        setChecked(pref.getBoolean(key, defaultValue))

        switch.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean(key, isChecked).apply()
        }

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

    fun isChecked(): Boolean {
        return switch.isChecked
    }

    fun setChecked(checked: Boolean) {
        switch.isChecked = checked
    }

    fun toggle() {
        switch.toggle()
    }
}
