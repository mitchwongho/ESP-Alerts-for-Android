package za.co.mitchwongho.example.esp32.alerts

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
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

    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are *not* resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * [.onResumeFragments].
     */
    override fun onResume() {
        super.onResume()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val names: Array<String> = installedApps.map { applicationInfo -> packageManager.getApplicationLabel(applicationInfo).toString() }.toTypedArray()
        fab.setOnClickListener({ view ->

            val prefsAllowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())
            val checkedItems: BooleanArray = BooleanArray(installedApps.size)
            for (i in names.indices) {
                checkedItems[i] = prefsAllowedPackages.contains(installedApps[i].packageName)
            }

            val modifiedList: ArrayList<String> = arrayListOf<String>()
            modifiedList.addAll(prefsAllowedPackages)


            // 885e2ece
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


        })

    }
}
