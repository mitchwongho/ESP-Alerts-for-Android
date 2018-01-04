package za.co.mitchwongho.example.esp32.alerts.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager

/**
 * Invoked after the system boots up
 */
class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val startAtBoot = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.PREF_KEY_START_AT_BOOT, false)
        if (startAtBoot) {
            val intent = Intent(context, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

    }
}