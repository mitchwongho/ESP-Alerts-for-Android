package za.co.mitchwongho.example.esp32.alerts

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import timber.log.Timber
import za.co.mitchwongho.example.esp32.alerts.app.ForegroundService
import za.co.mitchwongho.example.esp32.alerts.app.MainApplication
import za.co.mitchwongho.example.esp32.alerts.app.SettingsActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var fab: FloatingActionButton
    lateinit var menu: Menu
    var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab = findViewById(R.id.fab)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        fab.setOnClickListener({ view ->

            val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(BuildConfig.APPLICATION_ID)
            Timber.d("Notification Listener Enabled $enabled")

            if (alertDialog == null || !(alertDialog!!.isShowing)) {
                if (enabled) {

                    // lookup installed apps
                    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    installedApps.sortWith(kotlin.Comparator { a, b ->
                        val nameA = packageManager.getApplicationLabel(a).toString()
                        val nameB = packageManager.getApplicationLabel(b).toString()
                        nameA.compareTo(nameB)
                    })
                    val names: Array<String> = installedApps.map { applicationInfo -> packageManager.getApplicationLabel(applicationInfo).toString() }.toTypedArray()

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
                            .setOnDismissListener { dialogInterface -> alertDialog = null }
                            .setOnCancelListener { dialogInterface -> alertDialog = null }
                    alertDialog = builder.create()
                    alertDialog!!.show()
                } else {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                            .setTitle(R.string.choose_app)
                            .setMessage("Looks like you must first grant this app access to notifications. Do you want to continue?")
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                                if (!enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } else {
                                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                    }
                                }
                            })
                            .setOnDismissListener { dialogInterface -> alertDialog = null }
                            .setOnCancelListener { dialogInterface -> alertDialog = null }
                    alertDialog = builder.create()
                    alertDialog!!.show()

                }
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_prefs -> {
                showPreferences()
                true
            }
            R.id.menu_item_kill -> {
                stopService(Intent(this, ForegroundService::class.java))
                item.setVisible(false)
                menu.findItem(R.id.menu_item_start)?.setVisible(true)
                true
            }
            R.id.menu_item_start -> {
                startService(Intent(this, ForegroundService::class.java))
                item.setVisible(false)
                menu.findItem(R.id.menu_item_kill)?.setVisible(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, ForegroundService::class.java))
        Timber.w("onStart")
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop the service
        val isRunAsAService = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.PREF_KEY_RUN_AS_A_SERVICE, false)
        Timber.w("onDestroy {isService=$isRunAsAService}")
        if (!isRunAsAService) {
            stopService(Intent(this, ForegroundService::class.java))
        }
    }

    fun showPreferences() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
