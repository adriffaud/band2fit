package fr.driffaud.band2fit

import android.content.Context
import android.content.SharedPreferences
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
        LOG.info("doWork")

        val doSync = sharedPrefs.getBoolean("sync_influx", false)

        if (!doSync) {
            LOG.info("Sync is disabled")
            return Result.success()
        }

        val exportFile = sharedPrefs.getString("gadget_path", "")
        if (exportFile!!.isEmpty()) {
            LOG.info("Invalid export file")
            return Result.failure()
        }

        var database: GadgetDbOpenHelper? = null
        try {
            database = GadgetDbOpenHelper(applicationContext, exportFile)
            val datapoints = database.getDatapoints()

            val lastTs = sharedPrefs.getLong("lastTs", 0)
            val toSend = datapoints.filter { it.timestamp > lastTs }
            LOG.info("Sending ${toSend.size} datapoints from ${datapoints.size} (last stored ts: $lastTs)")

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
                LOG.info("Server response unsuccessful: ${res.code()}")
                Result.failure()
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
            return Result.failure()
        } finally {
            database?.close()
        }
    }

    private fun setLastSync() {
        val df = DateFormat.getTimeInstance(DateFormat.SHORT)
        val lastSync = df.format(Date())

        LOG.info("Last sync: $lastSync")
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