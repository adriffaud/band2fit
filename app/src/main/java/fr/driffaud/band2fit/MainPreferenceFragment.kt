package fr.driffaud.band2fit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val READ_REQUEST_CODE: Int = 42
private const val SYNC_FREQUENCY: Long = 1
const val TAG: String = "MiBandExporter"

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
        sharedPrefs.registerOnSharedPreferenceChangeListener { sharedPreferences, _ ->
            val doSync = sharedPreferences.getBoolean("sync_influx", false)
            val serverUrl = sharedPreferences.getString("server_url", "")
            val username = sharedPreferences.getString("username", "")
            val password = sharedPreferences.getString("password", "")

            Log.i(TAG, "doSync: $doSync, serverUrl: $serverUrl, username: $username")
            if (doSync && serverUrl!!.isNotEmpty() && username!!.isNotEmpty() && password!!.isNotEmpty()) {
                Log.i(TAG, "Register work")
                val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadWorker>(SYNC_FREQUENCY, TimeUnit.HOURS).build()
                WorkManager.getInstance().enqueue(uploadWorkRequest)
            } else {
                Log.i(TAG, "Cancel work")
                WorkManager.getInstance().cancelAllWork()
            }
        }

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