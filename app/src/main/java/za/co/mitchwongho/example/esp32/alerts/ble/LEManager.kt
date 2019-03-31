package za.co.mitchwongho.example.esp32.alerts.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.preference.PreferenceManager
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.Request
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.FailCallback
import no.nordicsemi.android.ble.callback.MtuCallback
import no.nordicsemi.android.ble.callback.SuccessCallback
import timber.log.Timber
import za.co.mitchwongho.example.esp32.alerts.app.ForegroundService
import za.co.mitchwongho.example.esp32.alerts.app.SettingsActivity
import java.util.*

/**
 * Implements BLEManager
 */
class LEManager : BleManager<LeManagerCallbacks> {

    var espDisplayMessageCharacteristic: BluetoothGattCharacteristic? = null
    var espDisplayTimeCharacteristic: BluetoothGattCharacteristic? = null
    var espDisplayOrientationCharacteristic: BluetoothGattCharacteristic? = null

    companion object {
        val MTU = 500
        val ESP_SERVICE_UUID = UUID.fromString("3db02924-b2a6-4d47-be1f-0f90ad62a048")
        val ESP_DISPLAY_MESSAGE_CHARACTERISITC_UUID = UUID.fromString("8d8218b6-97bc-4527-a8db-13094ac06b1d")
        val ESP_DISPLAY_TIME_CHARACTERISITC_UUID = UUID.fromString("b7b0a14b-3e94-488f-b262-5d584a1ef9e1")
        val ESP_DISPLAY_ORIENTATION_CHARACTERISITC_UUID = UUID.fromString("0070b87e-d825-43f5-be0c-7d86f75e4900")

        fun readBatteryLevel(context: Context): Int {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            val batteryLevelPercent: Int = ((level.toFloat() / scale.toFloat()) * 100f).toInt()
            Timber.d("readTimeAndBatt {level=$level,scale=$scale,batteryLevel=$batteryLevelPercent%}")
            return batteryLevelPercent
        }
    }

    constructor(context: Context) : super(context)

    /**
     * This method must return the gatt callback used by the manager.
     * This method must not create a new gatt callback each time it is being invoked, but rather return a single object.
     *
     * @return the gatt callback object
     */
    override fun getGattCallback(): BleManagerGattCallback {
        return callback
    }

    /**
     * Write {@code message} to the remote device's characteristic
     */
    fun writeNotification(message: String): Boolean {
        return write(message)
    }

    /**
     * Write {@code message} to the remote device's characteristic
     */
    fun writeTimeAndBatt(message: String): Boolean {
        //
        // read battery level
        val batteryLevelPercent = Companion.readBatteryLevel(context)
        return writeTimeAndBatteryLevel(batteryLevelPercent, message)
    }

    private fun write(message: String): Boolean {
        Timber.d("write {connected=$isConnected,hasCharacteristic=${espDisplayMessageCharacteristic != null}}")
        return if (isConnected && espDisplayMessageCharacteristic != null) {
            requestMtu(MTU).enqueue()
            writeCharacteristic(espDisplayMessageCharacteristic, message.toByteArray()).enqueue()
            true
        } else {
            false
        }
    }
    private fun writeTimeAndBatteryLevel(battLevel: Int, message: String): Boolean {
        Timber.d("write {connected=$isConnected,hasCharacteristic=${espDisplayMessageCharacteristic != null}}")
        return if (isConnected && espDisplayTimeCharacteristic != null) {
            requestMtu(MTU).enqueue()
            writeCharacteristic(espDisplayTimeCharacteristic, (battLevel.toChar() + message).toByteArray()).enqueue()
            true
        } else {
            false
        }
    }

    fun applyDisplayVertifically(): Boolean {
        return if (isConnected && espDisplayOrientationCharacteristic != null) {
            val displayOrientation = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(SettingsActivity.PREF_KEY_FLIP_DISPLAY_VERTICALLY, false)
            val flag = if (displayOrientation) 1 else 2
            val barray = ByteArray(1)
            barray.set(0, flag.toByte())
            writeCharacteristic(espDisplayOrientationCharacteristic, barray).enqueue()
            true
        } else {
            false
        }
    }

    /**
     * Returns whether to connect to the remote device just once (false) or to add the address to white list of devices
     * that will be automatically connect as soon as they become available (true). In the latter case, if
     * Bluetooth adapter is enabled, Android scans periodically for devices from the white list and if a advertising packet
     * is received from such, it tries to connect to it. When the connection is lost, the system will keep trying to reconnect
     * to it in. If true is returned, and the connection to the device is lost the [BleManagerCallbacks.onLinklossOccur]
     * callback is called instead of [BleManagerCallbacks.onDeviceDisconnected].
     *
     * This feature works much better on newer Android phone models and many not work on older phones.
     *
     * This method should only be used with bonded devices, as otherwise the device may change it's address.
     * It will however work also with non-bonded devices with private static address. A connection attempt to
     * a device with private resolvable address will fail.
     *
     * The first connection to a device will always be created with autoConnect flag to false
     * (see [BluetoothDevice.connectGatt]). This is to make it quick as the
     * user most probably waits for a quick response. However, if this method returned true during first connection and the link was lost,
     * the manager will try to reconnect to it using [BluetoothGatt.connect] which forces autoConnect to true .
     *
     * @return autoConnect flag value
     */
    override fun shouldAutoConnect(): Boolean {
        return true
    }

    /**
     * Implements GATTCallback methods
     */
    private val callback: BleManagerGattCallback = object : BleManagerGattCallback() {
        /**
         * This method should return `true` when the gatt device supports the required services.
         *
         * @param gatt the gatt device with services discovered
         * @return `true` when the device has the required service
         */
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val gattService: BluetoothGattService? = gatt.getService(ESP_SERVICE_UUID)
            if (espDisplayMessageCharacteristic == null) {
                gattService?.getCharacteristic(ESP_DISPLAY_MESSAGE_CHARACTERISITC_UUID)?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                espDisplayMessageCharacteristic = gattService?.getCharacteristic(ESP_DISPLAY_MESSAGE_CHARACTERISITC_UUID)
            }
            if (espDisplayTimeCharacteristic == null) {
                espDisplayTimeCharacteristic = gattService?.getCharacteristic(ESP_DISPLAY_TIME_CHARACTERISITC_UUID)
            }
            if (espDisplayOrientationCharacteristic == null) {
                espDisplayOrientationCharacteristic = gattService?.getCharacteristic(ESP_DISPLAY_ORIENTATION_CHARACTERISITC_UUID)
            }
            return gattService != null
                    && espDisplayMessageCharacteristic != null
                    && espDisplayTimeCharacteristic != null
                    && espDisplayOrientationCharacteristic != null
        }


        /**
         * This method should set up the request queue needed to initialize the profile.
         * Enabling Service Change indications for bonded devices is handled before executing this
         * queue. The queue may have requests that are not available, e.g. read an optional
         * service when it is not supported by the connected device. Such call will trigger
         * {@link Request#fail(FailCallback)}.
         * <p>
         * This method is called from the main thread when the services has been discovered and
         * the device is supported (has required service).
         * <p>
         * Remember to call {@link Request#enqueue()} for each request.
         * <p>
         * A sample initialization should look like this:
         * <pre>
         * &#64;Override
         * protected void initialize() {
         *    requestMtu(MTU)
         *       .with((device, mtu) -> {
         *           ...
         *       })
         *       .enqueue();
         *    setNotificationCallback(characteristic)
         *       .with((device, data) -> {
         *           ...
         *       });
         *    enableNotifications(characteristic)
         *       .done(device -> {
         *           ...
         *       })
         *       .fail((device, status) -> {
         *           ...
         *       })
         *       .enqueue();
         * }
         * </pre>
         */
        override fun initialize() {
            Timber.i("Initialising...")

            enableNotifications(espDisplayTimeCharacteristic)
                    .done(SuccessCallback {
                        Timber.i("Successfully enabled DisplayMessageCharacteristic notifications")
                    })
                    .fail { device, status ->
                        Timber.w("Failed to enable DisplayMessageCharacteristic notifications")
                    }.enqueue()
            enableIndications(espDisplayMessageCharacteristic)
                    .done(SuccessCallback {
                        Timber.i("Successfully wrote message")
                    })
                    .fail(FailCallback { device, status ->
                        Timber.w("Failed to write message to ${device.address} - status: ${status}")
                    })
                    .enqueue()

//            requestMtu(MTU).enqueue()
            setNotificationCallback(espDisplayTimeCharacteristic)
                    .with(DataReceivedCallback { device, data ->
                        Timber.i("Data received from ${device.address}")
                    })
            enableNotifications(espDisplayTimeCharacteristic)
                    .done(SuccessCallback {
                        Timber.i("Successfully enabled DisplayTimeCharacteristic notifications")
                    })
                    .fail { device, status ->
                        Timber.w("Failed to enable DisplayTimeCharacteristic notifications")
                    }.enqueue()
            enableIndications(espDisplayTimeCharacteristic)
                    .done(SuccessCallback {
                        Timber.i("Successfully wrote Time & Battery status")
                    })
                    .fail(FailCallback { device, status ->
                        Timber.w("Failed to write Time & Battery status to ${device.address} - status: ${status}")
                    }).enqueue()

            val batteryLevelPercent = Companion.readBatteryLevel(context)
            writeTimeAndBatteryLevel(batteryLevelPercent, ForegroundService.formatter.format(Date()))
        }

        /**
         * This method should nullify all services and characteristics of the device.
         * It's called when the device is no longer connected, either due to user action
         * or a link loss.
         */
        override fun onDeviceDisconnected() {
            espDisplayMessageCharacteristic = null
            espDisplayTimeCharacteristic = null
            espDisplayOrientationCharacteristic = null
        }
    }
}