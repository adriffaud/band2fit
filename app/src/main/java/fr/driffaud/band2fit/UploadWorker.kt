package fr.driffaud.band2fit

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber
import java.text.DateFormat
import java.util.*


class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    override fun doWork(): Result {
        Timber.i("doWork")

        val doSync = sharedPrefs.getBoolean("sync_influx", false)

        if (!doSync) {
            Timber.i("Sync is disabled")
            return Result.success()
        }

        val exportFile = sharedPrefs.getString("gadget_path", "")
        if (exportFile!!.isEmpty()) {
            Timber.i("Invalid export file")
            return Result.failure()
        }

        var database: GadgetDbOpenHelper? = null
        try {
            database = GadgetDbOpenHelper(applicationContext, exportFile)
            val datapoints = database.getDatapoints()

            val lastTs = sharedPrefs.getLong("lastTs", 0)
            val filtered = datapoints.filter { it.timestamp > lastTs }
            val toSend = filtered.take(80000)
            Timber.i("Sending ${toSend.size} datapoints from ${filtered.size} (last stored ts: $lastTs)")

            val remaining = filtered.size - toSend.size > 0
            Timber.i("${filtered.size - toSend.size} datapoints remaining")

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

                if (remaining) this.doWork()

                Result.success()
            } else {
                Timber.i("Server response unsuccessful: ${res.code}")
                Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e)
            return Result.failure()
        } finally {
            database?.close()
        }
    }

    private fun setLastSync() {
        val df = DateFormat.getTimeInstance(DateFormat.SHORT)
        val lastSync = df.format(Date())

        Timber.i("Last sync: $lastSync")
        sharedPrefs.edit()
            .putString("lastSync", lastSync)
            .apply()
    }

    private fun sendToInflux(datapoints: List<Datapoint>): Response? {
        val gson = Gson()
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = gson.toJson(datapoints).toRequestBody(mediaType)

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