package za.co.mitchwongho.example.esp32.alerts.app

import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import za.co.mitchwongho.example.esp32.alerts.R
import java.util.regex.Pattern

/**
 *
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        val PREF_KEY_RUN_AS_A_SERVICE = "pref_as_bg_service"
        val PREF_KEY_REMOTE_MAC_ADDRESS = "pref_remote_mac_address"
        val PREF_KEY_START_AT_BOOT = "pref_start_at_boot"
        val PREF_KEY_FLIP_DISPLAY_VERTICALLY = "pref_flip_vertically"
        val MAC_PATTERN = Pattern.compile("^([A-F0-9]{2}[:]?){5}[A-F0-9]{2}$")

        class SettingsFragment : PreferenceFragment() {

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                addPreferencesFromResource(R.xml.preferences)
                //
                // apply persisted value
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
                setRemoteMACAddressPrefSummary(sharedPref.getString(PREF_KEY_REMOTE_MAC_ADDRESS, "00:00:00:00:00:00"))
                //
                // validate updates and apply is valid
                findPreference(PREF_KEY_REMOTE_MAC_ADDRESS).setOnPreferenceChangeListener({ preference: Preference?, value: Any? ->
                    val mac = (value as String).trim()
                    if (MAC_PATTERN.matcher(mac).find()) {
                        setRemoteMACAddressPrefSummary(mac)
                        true
                    } else {
                        Toast.makeText(activity, R.string.mac_format_error, Toast.LENGTH_LONG).show()
                        false
                    }
                })
            }

            fun setRemoteMACAddressPrefSummary(summary: String) {
                val pref = findPreference(PREF_KEY_REMOTE_MAC_ADDRESS)
                pref.summary = summary
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsActivity.Companion.SettingsFragment())
                .commit()
    }

    override fun onStart() {
        super.onStart()
        stopService(Intent(this, ForegroundService::class.java))
    }
}