package fr.driffaud.band2fit

import android.os.Bundle

class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, MainPreferenceFragment()).commit()
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return MainPreferenceFragment::class.java.name == fragmentName
    }
}
