package fr.driffaud.band2fit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

private const val READ_REQUEST_CODE: Int = 42

class PreferenceFragment : PreferenceFragmentCompat() {

    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                with(preferenceManager.sharedPreferences.edit()) {
                    putString("gadget_path", uri.toString())
                    apply()
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val sharedPrefs = preferenceManager.sharedPreferences

        val pathSummaryProvider = Preference.SummaryProvider<Preference> {
            if (sharedPrefs.getString("gadget_path", "")!!.isNotEmpty()) {
                "Export path is set"
            } else {
                "Select the Gadgetbridge exported file path"
            }
        }
        val pathClickListener = Preference.OnPreferenceClickListener {
            performFileSearch()
            true
        }
        val gadgetPathBtn = findPreference<Preference>("gadget_path")
        gadgetPathBtn?.onPreferenceClickListener = pathClickListener
        gadgetPathBtn?.summaryProvider = pathSummaryProvider

        val urlEditText = findPreference<EditTextPreference>("server_url")
        urlEditText?.onBindEditTextListener = EditTextPreference.OnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        val userEditText = findPreference<EditTextPreference>("username")
        userEditText?.onBindEditTextListener = EditTextPreference.OnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        val passEditText = findPreference<EditTextPreference>("password")
        passEditText?.onBindEditTextListener = EditTextPreference.OnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            it.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        val lastSyncTime = "09:14"
        val syncSummaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> {
            if (it.isChecked) {
                "Last synced at $lastSyncTime"
            } else {
                "Syncing is currently disabled"
            }
        }
        findPreference<SwitchPreferenceCompat>("sync_influx")?.summaryProvider = syncSummaryProvider
    }
}