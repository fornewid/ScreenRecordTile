package androidx.preference

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import soup.tile.screenrecord.R

class SwitchPreferenceCompatEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceCompatStyle,
    defStyleRes: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    private val dependencyChecked: Boolean

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.SwitchPreferenceCompatEx, defStyleAttr, defStyleRes
        )

        dependencyChecked = TypedArrayUtils.getBoolean(
            a,
            R.styleable.SwitchPreferenceCompatEx_dependencyChecked,
            R.styleable.SwitchPreferenceCompatEx_dependencyChecked,
            false
        )

        a.recycle()
    }

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        super.onDependencyChanged(dependency, disableDependent)
        if (disableDependent) {
            isChecked = dependencyChecked
        }
    }
}
