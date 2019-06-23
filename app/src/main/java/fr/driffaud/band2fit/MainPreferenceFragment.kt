package fr.driffaud.band2fit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val READ_REQUEST_CODE: Int = 42

class PreferenceFragment : PreferenceFragmentCompat() {

    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                // Ensure we keep file access permission across phone restarts
                val takeFlags =
                    resultData.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)

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
        sharedPrefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            Timber.d("Shared pref change: $key")
            val exportFilePath = sharedPreferences.getString("gadget_path", "")
            val doSync = sharedPreferences.getBoolean("sync_influx", false)
            val serverUrl = sharedPreferences.getString("server_url", "")
            val username = sharedPreferences.getString("username", "")
            val password = sharedPreferences.getString("password", "")

            if (doSync && exportFilePath!!.isNotEmpty() && serverUrl!!.isNotEmpty() && username!!.isNotEmpty() && password!!.isNotEmpty()) {
                Timber.i("Register work")
                val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadWorker>(45, TimeUnit.MINUTES).build()
                WorkManager.getInstance()
                    .enqueueUniquePeriodicWork("MIBAND_SYNC", ExistingPeriodicWorkPolicy.KEEP, uploadWorkRequest)
            } else {
                Timber.i("Cancelling all work")
                WorkManager.getInstance().cancelAllWork()
            }

            updateSyncTime(sharedPreferences)
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
        urlEditText?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        val userEditText = findPreference<EditTextPreference>("username")
        userEditText?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        val passEditText = findPreference<EditTextPreference>("password")
        passEditText?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            it.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        updateSyncTime(sharedPrefs)
    }

    private fun updateSyncTime(sharedPreferences: SharedPreferences) {
        Timber.d("Update sync time")
        val doSync = sharedPreferences.getBoolean("sync_influx", false)
        val lastSyncTime = sharedPreferences.getString("lastSync", "")
        val syncPref = findPreference<SwitchPreferenceCompat>("sync_influx")

        if (doSync && lastSyncTime!!.isNotEmpty()) {
            syncPref?.summary = "Last synced at $lastSyncTime"
        } else if (doSync && lastSyncTime!!.isEmpty()) {
            syncPref?.summary = "Not synced yet"
        } else {
            syncPref?.summary = "Syncing disabled"
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission_group.STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission_group.STORAGE), 0)
        }
    }
}
