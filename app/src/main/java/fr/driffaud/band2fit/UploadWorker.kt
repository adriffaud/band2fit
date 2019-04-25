package fr.driffaud.band2fit

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class UploadWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Do the work here--in this case, upload the images.
        Log.i(TAG, "doWork")

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val doSync = sharedPrefs.getBoolean("sync_influx", false)
        Log.i(TAG, "Sync: $doSync")

        if (!doSync) {
            Log.i(TAG, "Sync is disabled")
            return Result.success()
        }

        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }

}