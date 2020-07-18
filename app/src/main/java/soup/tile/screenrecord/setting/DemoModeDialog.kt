package soup.tile.screenrecord.setting

import android.app.AlertDialog
import android.content.Context
import soup.tile.screenrecord.R

object DemoModeDialog {

    fun show(context: Context) {
        AlertDialog.Builder(context, R.style.AppTheme_Dialog)
            .setTitle(R.string.dialog_demo_mode_title)
            .setMessage(R.string.dialog_demo_mode_desc)
            .setView(R.layout.dialog_demo_mode)
            .setNegativeButton(R.string.dialog_demo_mode_button) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
