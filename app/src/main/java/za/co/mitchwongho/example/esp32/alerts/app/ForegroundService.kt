package za.co.mitchwongho.example.esp32.alerts.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import timber.log.Timber
import za.co.mitchwongho.example.esp32.alerts.BuildConfig
import za.co.mitchwongho.example.esp32.alerts.MainActivity
import za.co.mitchwongho.example.esp32.alerts.R
import za.co.mitchwongho.example.esp32.alerts.ble.LEManager
import za.co.mitchwongho.example.esp32.alerts.ble.LeManagerCallbacks
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
class ForegroundService : Service() {

    companion object {
        val TAG = ForegroundService::class.java.simpleName
        val SERVICE_ID = 9001
        val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID
        val VESPA_DEVICE_ADDRESS = "24:0A:C4:13:58:EA" // <--- YOUR ESP32 MAC address here
        val formatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
    }

    lateinit var bleManager: BleManager<LeManagerCallbacks>
    var lastPost: Long = 0L
    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    override fun onCreate() {
        super.onCreate()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val leDevice = bluetoothManager.adapter.getRemoteDevice(VESPA_DEVICE_ADDRESS)
        bleManager = LEManager(this)
        bleManager.setGattCallbacks(bleManagerCallback)
        bleManager.connect(leDevice)

        val intentFilter = IntentFilter(NotificationListener.EXTRA_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter)

        registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)
        unregisterReceiver(tickReceiver)
        super.onDestroy()
    }

    /**
     * Create/Update the notification
     */
    private fun notify(contentText: String): Notification {
        // Launch the MainAcivity when user taps on the Notification
        val pendingIntent = PendingIntent.getActivity(this, 0
                , Intent(this, MainActivity::class.java)
                , PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_espressif)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(SERVICE_ID, notification)
        return notification
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) {
            //service restarted
        } else {
            //started by intent or pending intent
            val notification = notify("Scanning...")
            startForeground(SERVICE_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    var bleManagerCallback: LeManagerCallbacks = object : LeManagerCallbacks() {
        /**
         * Called when the device has been connected. This does not mean that the application may start communication.
         * A service discovery will be handled automatically after this call. Service discovery
         * may ends up with calling [.onServicesDiscovered] or
         * [.onDeviceNotSupported] if required services have not been found.
         * @param device the device that got connected
         */
        override fun onDeviceConnected(device: BluetoothDevice) {
            super.onDeviceConnected(device)
            notify("Connected to ${device.name}")
//            val success = (bleManager as LEManager).writeTime(formatter.format(Date()))
//            Timber.d("writeTime {success=$success}")
        }

        /**
         * Called when the Android device started connecting to given device.
         * The [.onDeviceConnected] will be called when the device is connected,
         * or [.onError] in case of error.
         * @param device the device that got connected
         */
        override fun onDeviceConnecting(device: BluetoothDevice) {
            super.onDeviceConnecting(device)
            notify("Connecting to ${ if (device.name.isNullOrEmpty()) "device" else device.name }")
        }

        /**
         * Called when user initialized disconnection.
         * @param device the device that gets disconnecting
         */
        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            super.onDeviceDisconnecting(device)
            notify("Disconnecting from ${device.name}")
        }

        /**
         * Called when the device has disconnected (when the callback returned
         * [BluetoothGattCallback.onConnectionStateChange] with state DISCONNECTED),
         * but ONLY if the [BleManager.shouldAutoConnect] method returned false for this device when it was connecting.
         * Otherwise the [.onLinklossOccur] method will be called instead.
         * @param device the device that got disconnected
         */
        override fun onDeviceDisconnected(device: BluetoothDevice) {
            super.onDeviceDisconnected(device)
            notify("Disconnected from ${device.name}")
        }

        /**
         * This callback is invoked when the Ble Manager lost connection to a device that has been connected
         * with autoConnect option (see [BleManager.shouldAutoConnect].
         * Otherwise a [.onDeviceDisconnected] method will be called on such event.
         * @param device the device that got disconnected due to a link loss
         */
        override fun onLinklossOccur(device: BluetoothDevice) {
            super.onLinklossOccur(device)
            notify("Lost link to ${device.name}")
        }
    }

    var tickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (System.currentTimeMillis() - lastPost > (2 * 60 * 1000)) {
                (bleManager as LEManager).writeTime(formatter.format(Date()))
            }
        }
    }

    var localReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (bleManager.isConnected && intent != null) {
                Log.d(TAG, "onReceive")
                val notificationId = intent.getIntExtra(NotificationListener.EXTRA_NOTIFICATION_ID_INT, 0)
                val notificationAppName = intent.getStringExtra(NotificationListener.EXTRA_APP_NAME)
                val notificationTitle = intent.getStringExtra(NotificationListener.EXTRA_TITLE)
                val notificationBody = intent.getStringExtra(NotificationListener.EXTRA_BODY)
                val notificationTimestamp = intent.getLongExtra(NotificationListener.EXTRA_TIMESTAMP_LONG, 0)
                val notificationDismissed = intent.getBooleanExtra(NotificationListener.EXTRA_NOTIFICATION_DISMISSED, true)
                //
                if (notificationDismissed) {
                    val success = (bleManager as LEManager).writeTime(formatter.format(Date()))
                    lastPost = notificationTimestamp
                    Timber.d("writeTime {success=$success}")
                } else {
                    val buffer = StringBuffer(256)
                    buffer.append(notificationTitle)
                    buffer.append(":\"")
                    buffer.append(notificationBody)
                    buffer.append("\" via ")
                    buffer.append(notificationAppName).append(" @ ")
                    buffer.append(formatter.format(Date(notificationTimestamp)))
                    val success = (bleManager as LEManager).writeMessage(buffer.substring(0, Math.min(buffer.length, 256)))
                    lastPost = notificationTimestamp
                    Timber.d("writeMessage {success=$success}")
                }
            }
        }
    }

}