package fr.driffaud.band2fit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

private const val DB_VERSION: Int = 1
private const val DB_NAME: String = "tempDb"

private const val MIBAND_DATAPOINTS: String =
    "SELECT TIMESTAMP, RAW_INTENSITY, STEPS, RAW_KIND, HEART_RATE FROM MI_BAND_ACTIVITY_SAMPLE ORDER BY TIMESTAMP ASC"

class GadgetDbOpenHelper(context: Context, dbName: String) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val dir: File = context.getExternalFilesDirs(null)[0]
    private var database: SQLiteDatabase?

    init {
        val destDb = File(dir, databaseName)

        Timber.i("Trying to copy database to $destDb")
        context.contentResolver.openInputStream(Uri.parse(dbName))?.use { it.copyTo(FileOutputStream(destDb), 4096) }

        if (!destDb.exists()) {
            throw Exception("Couldn't copy database file to ${destDb.absolutePath}")
        }
        Timber.i("Successfully copied database")

        database = SQLiteDatabase.openDatabase(destDb.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
    }

    fun getDatapoints(): List<Datapoint> {
        if (!database?.isOpen!!) {
            throw Exception("Couldn't open database")
        }

        val datapoints: MutableList<Datapoint> = mutableListOf()
        database?.rawQuery(MIBAND_DATAPOINTS, null).use { cursor ->
            Timber.i("${cursor?.count} rows to read")

            if (cursor!!.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val dp = Datapoint(
                        cursor.getLong(cursor.getColumnIndex("TIMESTAMP")),
                        cursor.getInt(cursor.getColumnIndex("RAW_INTENSITY")),
                        cursor.getInt(cursor.getColumnIndex("STEPS")),
                        cursor.getInt(cursor.getColumnIndex("RAW_KIND")),
                        cursor.getInt(cursor.getColumnIndex("HEART_RATE"))
                    )
                    datapoints.add(dp)
                    cursor.moveToNext()
                }
            }
        }
        return datapoints
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        return database!!
    }

    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

}