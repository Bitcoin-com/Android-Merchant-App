package com.bitcoin.merchant.app.screens.dialogs

import android.app.AlertDialog
import android.text.InputFilter.LengthFilter
import android.widget.EditText
import android.widget.TextView
import com.bitcoin.merchant.app.MainActivity
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.util.Settings

class MerchantNameEditorDialog(private val activity: MainActivity) {
    fun show(namePref: TextView): Boolean {
        val etName = EditText(activity)
        etName.isSingleLine = true
        etName.filters = arrayOf(LengthFilter(70))
        etName.setText(Settings.getMerchantName(activity))
        val builder = AlertDialog.Builder(activity)
                .setTitle(R.string.settings_merchant_name)
                .setView(etName)
                .setCancelable(false)
                .setPositiveButton(R.string.prompt_ok) { dialog, whichButton ->
                    val name = etName.text.toString()
                    if (name.isNotEmpty()) {
                        if (Settings.getMerchantName(activity) != name) {
                            Settings.setMerchantName(activity, name)
                            SnackHelper.show(activity, activity.getString(R.string.notify_changes_have_been_saved))
                        }
                        namePref.text = name
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.prompt_ko) { dialog, whichButton -> dialog.dismiss() }
        if (!activity.isFinishing) {
            builder.show()
        }
        return true
    }

}