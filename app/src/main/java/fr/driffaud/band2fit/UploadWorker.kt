package fr.driffaud.band2fit

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import okhttp3.*

class UploadWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    override fun doWork(): Result {
        Log.i(TAG, "doWork")

        val doSync = sharedPrefs.getBoolean("sync_influx", false)

        if (!doSync) {
            Log.i(TAG, "Sync is disabled")
            return Result.success()
        }

        val exportFile = sharedPrefs.getString("gadget_path", "")
        if (exportFile!!.isEmpty()) {
            Log.i(TAG, "Invalid export file")
            return Result.failure()
        }

        val datapoints = GadgetDbOpenHelper(applicationContext, exportFile).getDatapoints()
        Log.i(TAG, "Datapoints: ${datapoints.size}")

        // Send to server
        try {
            val res = sendToInflux(datapoints)
            if (!res?.isSuccessful!!) {
                return Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            return Result.failure()
        }

        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }

    private fun sendToInflux(datapoints: List<Datapoint>): Response? {
        val gson = Gson()
        val client = OkHttpClient()
        val body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), gson.toJson(datapoints))

        val url = sharedPrefs.getString("server_url", "")
        val username = sharedPrefs.getString("username", "")
        val password = sharedPrefs.getString("password", "")

        if (url!!.isEmpty() || username!!.isEmpty() || password!!.isEmpty()) {
            return null
        }

        val credentials = Credentials.basic(username, password)
        val request = Request.Builder()
            .header("Authorization", credentials)
            .url(url)
            .post(body)
            .build()

        return client.newCall(request).execute()
    }
}