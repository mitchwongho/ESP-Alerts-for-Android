package za.co.mitchwongho.example.esp32.alerts.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.content.LocalBroadcastManager
import android.text.SpannableString
import android.util.Log
import timber.log.Timber

/**
 *
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        val EXTRA_ACTION = "ESP"
        val EXTRA_NOTIFICATION_DISMISSED = "EXTRA_NOTIFICATION_DISMISSED"
        val EXTRA_APP_NAME = "EXTRA_APP_NAME"
        val EXTRA_NOTIFICATION_ID_INT = "EXTRA_NOTIFICATION_ID_INT"
        val EXTRA_TITLE = "EXTRA_TITLE"
        val EXTRA_BODY = "EXTRA_BODY"
        val EXTRA_TIMESTAMP_LONG = "EXTRA_TIMESTAMP_LONG"
    }

    /**
     * Implement this method to learn about new notifications as they are posted by apps.
     *
     * @param sbn A data structure encapsulating the original [android.app.Notification]
     * object as well as its identifying information (tag and id) and source
     * (package name).
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val ticker = notification?.tickerText
        val bundle: Bundle? = notification?.extras
        val titleObj = bundle?.get("android.title")
        val title: String
        when (titleObj) {
            is String -> title = titleObj
            is SpannableString -> title = titleObj.toString()
            else -> title = "undefined"
        }
        val body: String? = bundle?.getCharSequence("android.text").toString()

        val appInfo = applicationContext.packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
        val appName = applicationContext.packageManager.getApplicationLabel(appInfo)
        Timber.d("onNotificationPosted {app=${appName},id=${sbn.id},ticker=$ticker,title=$title,body=$body,posted=${sbn.postTime},package=${sbn.packageName}}")

        val allowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())

        // broadcast StatusBarNotication (exclude own notifications)
        if (sbn.id != ForegroundService.SERVICE_ID
                && allowedPackages.contains(sbn.packageName)) {
            val intent = Intent(EXTRA_ACTION)
            intent.putExtra(EXTRA_NOTIFICATION_ID_INT, sbn.id)
            intent.putExtra(EXTRA_APP_NAME, appName)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_BODY, body)
            intent.putExtra(EXTRA_NOTIFICATION_DISMISSED, false)
            intent.putExtra(EXTRA_TIMESTAMP_LONG, sbn.postTime)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        val notification = sbn.notification
        val ticker = notification?.tickerText
        val bundle: Bundle? = notification?.extras
        val titleObj = bundle?.get("android.title")
        val title: String
        when (titleObj) {
            is String -> title = titleObj
            is SpannableString -> title = titleObj.toString()
            else -> title = "undefined"
        }
        val body: String? = bundle?.getCharSequence("android.text").toString()

        val appInfo = applicationContext.packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
        val appName = applicationContext.packageManager.getApplicationLabel(appInfo)
        Timber.d("onNotificationPosted {app=${appName},id=${sbn.id},ticker=$ticker,title=$title,body=$body,posted=${sbn.postTime},package=${sbn.packageName}}")

        val allowedPackages: MutableSet<String> = MainApplication.sharedPrefs.getStringSet(MainApplication.PREFS_KEY_ALLOWED_PACKAGES, mutableSetOf())

        Timber.d("onNotificationRemoved {app=${applicationContext.packageManager.getApplicationLabel(appInfo)},id=${sbn.id},ticker=$ticker,title=$title,body=$body,posted=${sbn.postTime},package=${sbn.packageName}}")

        // broadcast StatusBarNotication (exclude own notifications)
        if (sbn.id != ForegroundService.SERVICE_ID
                && allowedPackages.contains(sbn.packageName)) {
            val intent = Intent(EXTRA_ACTION)
            intent.putExtra(EXTRA_NOTIFICATION_ID_INT, sbn.id)
            intent.putExtra(EXTRA_APP_NAME, appName)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_BODY, body)
            intent.putExtra(EXTRA_NOTIFICATION_DISMISSED, true)
            intent.putExtra(EXTRA_TIMESTAMP_LONG, sbn.postTime)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }


}