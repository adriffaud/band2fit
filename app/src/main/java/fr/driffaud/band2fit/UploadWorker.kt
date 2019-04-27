package fr.driffaud.band2fit

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import okhttp3.*
import java.text.DateFormat
import java.util.*

class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

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

        var database: GadgetDbOpenHelper? = null
        try {
            database = GadgetDbOpenHelper(applicationContext, exportFile)
            val datapoints = database.getDatapoints()

            val lastTs = sharedPrefs.getLong("lastTs", 0)
            val toSend = datapoints.filter { it.timestamp > lastTs }
            Log.i(TAG, "Sending ${toSend.size} datapoints from ${datapoints.size} (last stored ts: $lastTs)")

            if (toSend.isEmpty()) {
                setLastSync()
                return Result.success()
            }

            val res = sendToInflux(toSend)
            return if (res?.isSuccessful!!) {
                setLastSync()
                sharedPrefs.edit()
                    .putLong("lastTs", toSend.last().timestamp)
                    .apply()
                Result.success()
            } else {
                Log.i(TAG, "Server response unsuccessful: ${res.code()}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            return Result.failure()
        } finally {
            database?.close()
        }
    }

    private fun setLastSync() {
        val df = DateFormat.getTimeInstance(DateFormat.SHORT)
        val lastSync = df.format(Date())

        Log.i(TAG, "Last sync: $lastSync")
        sharedPrefs.edit()
            .putString("lastSync", lastSync)
            .apply()
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