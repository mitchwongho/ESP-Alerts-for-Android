package za.co.mitchwongho.example.esp32.alerts

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.ArraySet
import android.view.View
import timber.log.Timber
import za.co.mitchwongho.example.esp32.alerts.app.ForegroundService
import za.co.mitchwongho.example.esp32.alerts.app.MainApplication
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this, ForegroundService::class.java))

        fab = findViewById(R.id.fab)

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val names: Array<String> = installedApps.map { applicationInfo -> packageManager.getApplicationLabel(applicationInfo).toString() }.toTypedArray()
        fab.setOnClickListener({ view ->

            val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(BuildConfig.APPLICATION_ID)
            Timber.d("Notification Listener Enabled $enabled")


            if (enabled) {

                // lookup installed apps
                val prefsAllowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())
                val checkedItems: BooleanArray = BooleanArray(installedApps.size)
                for (i in names.indices) {
                    checkedItems[i] = prefsAllowedPackages.contains(installedApps[i].packageName)
                }

                val modifiedList: ArrayList<String> = arrayListOf<String>()
                modifiedList.addAll(prefsAllowedPackages)

                // show Apps
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        .setTitle(R.string.choose_app)
                        .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialogInterface, i ->
                            // commit
                            MainApplication.sharedPrefs.edit().putStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, modifiedList.toSet()).commit();
                        })
                        .setNegativeButton(android.R.string.cancel, DialogInterface.OnClickListener { dialogInterface, i ->
                            // close without commit
                        })
                        .setMultiChoiceItems(names, checkedItems, DialogInterface.OnMultiChoiceClickListener { dialogInterface, position, checked ->
                            if (checked) {
                                modifiedList.add(installedApps[position].packageName)
                            } else {
                                modifiedList.remove(installedApps[position].packageName)
                            }
                        })
                builder.create().show()
            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        .setTitle(R.string.choose_app)
                        .setMessage("Looks like you must first grant this app access to notifications. Do you want to continue?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener {dialogInterface: DialogInterface?, i: Int ->
                            if (!enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } else {
                                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                }
                            }
                        })
                builder.create().show()

            }


        })

    }
}
