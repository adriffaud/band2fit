package fr.driffaud.band2fit

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import android.text.method.PasswordTransformationMethod


class PreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val urlEditText = findPreference<EditTextPreference>("server_url")
        urlEditText?.onBindEditTextListener = EditTextPreference.OnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        val userEditText = findPreference<EditTextPreference>("username")
        userEditText?.onBindEditTextListener = EditTextPreference.OnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        val passEditText = findPreference<EditTextPreference>("password")
        passEditText?.onBindEditTextListener = EditTextPreference.OnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        val lastSyncTime = "09:14"
        val syncSummaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> { preference ->
            if (preference.isChecked) {
                "Last synced at $lastSyncTime"
            } else {
                "Syncing is currently disabled"
            }
        }
        findPreference<SwitchPreferenceCompat>("sync_influx")?.summaryProvider = syncSummaryProvider
    }
}