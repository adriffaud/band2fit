package fr.driffaud.band2fit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // used by assets/logback.xml since the location cannot be statically determined
        val dir: File = applicationContext.getExternalFilesDirs(null)[0]
        System.setProperty("MI_EXPORTER_LOG_DIR", dir.absolutePath)

        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, PreferenceFragment())
            .commit()
    }

}
