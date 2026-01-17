package com.example.minimap32.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.minimap32.model.MAC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BleManager(
    private val context: Context
) {

    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var gatt: BluetoothGatt? = null
    private var isConnected = false

    private val SERVICE_UUID =
        UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    // Android -> ESP32 (WRITE)
    private val CMD_UUID =
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    // ESP32 -> Android (NOTIFY)
    private val STATUS_UUID =
        UUID.fromString("9d8c2d3a-7a12-4d3f-8f58-bc6b4f9c1123")

    // Descriptor
    private val CCCD_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Handle mac event
    private val _macEvents = MutableStateFlow<List<MAC>>(emptyList())
    val macEvents: StateFlow<List<MAC>> = _macEvents

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        // Check permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_SCAN
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                Log.e("BLE", "❌ Permission BLUETOOTH_SCAN manquante!")
                return
            }
        } else {
            // Android < 12 requires LOCATION permission for BLE scan
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e("BLE", "❌ Permission ACCESS_FINE_LOCATION manquante!")
                return
            }
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        Log.d("BLE", "🔍 Scan started")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        Log.d("BLE", "Scan stopped")
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(type: Int, result: ScanResult) {
            // Check CONNECT permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
            }

            val device = result.device
            val deviceName = device.name ?: "Unknown"

            Log.d("BLE", "Found device: $deviceName (RSSI: ${result.rssi})")

            if (deviceName == "Minimap32") {
                // Only connect if signal is strong enough
                if (result.rssi < -90) {
                    Log.d("BLE", "⚠️ Signal too weak (${result.rssi}), waiting...")
                    return
                }

                Log.d("BLE", "✅ ESP32 trouvé")
                bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                connect(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "❌ Scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.e("BLE", "❌ Permission BLUETOOTH_CONNECT manquante!")
                return
            }
        }

        // Close any existing connection
        gatt?.close()
        gatt = null

        // Small delay to let the BLE stack reset
        android.os.Handler(android.os.Looper.getMainLooper())
            .postDelayed(
                @androidx.annotation.RequiresPermission(
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) {
                    Log.d("BLE", "🔌 Connecting to device...")

                    // Try connection with autoConnect = true for more stable connection
                    gatt = device.connectGatt(
                        context,
                        true,  // Changed to true for auto-reconnect
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                }, 500
            )  // Increased delay to 500ms
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
            }

            when {
                status == 133 -> {
                    Log.e("BLE", "❌ Connection failed (status 133), retrying...")
                    gatt.close()
                    // Don't retry here - let user manually retry
                }

                newState == BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BLE", "🔗 Connected, requesting MTU...")
                    gatt.requestMtu(64)
                    Log.d("BLE", "🔗 Connecté, discovering services...")

                    isConnected = true
                }

                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BLE", "❌ Déconnecté")
                    isConnected = false
                    gatt.close()
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "✅ MTU negotiated: $mtu")
                gatt.discoverServices()
            } else {
                Log.e("BLE", "❌ MTU request failed")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "📡 Services discovered!")

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e("BLE", "❌ Service not found!")
                    return
                }

                val statusChar = service.getCharacteristic(STATUS_UUID)
                if (statusChar == null) {
                    Log.e("BLE", "❌ STATUS characteristic not found!")
                    return
                }

                // Enable notifications - USE NEW API
                gatt.setCharacteristicNotification(statusChar, true)

                val cccd = statusChar.getDescriptor(CCCD_UUID)
                if (cccd == null) {
                    Log.e("BLE", "❌ CCCD descriptor not found!")
                    return
                }

                // Use Android 13+ API or fallback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(cccd)
                }

                Log.d("BLE", "✅ Notifications enabled!")
            } else {
                Log.e("BLE", "❌ Service discovery failed: $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "✅ Descriptor written successfully")
            } else {
                Log.e("BLE", "❌ Descriptor write failed: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "✅ Characteristic written successfully")
            } else {
                Log.e("BLE", "❌ Characteristic write failed: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid != STATUS_UUID) return

            val msg = value.toString(Charsets.UTF_8)
            Log.d("BLE", "📥 NOTIFY: $msg")

            // Format: MAC,RSSI,CHANNEL
            val parts = msg.split(",")

            if (parts.size != 3) return

            val event = MAC(
                mac = parts[0],
                rssi = parts[1].toIntOrNull() ?: return,
                channel = parts[2].toIntOrNull() ?: return
            )

            // Déduplication simple côté Android
            _macEvents.value =
                (_macEvents.value + event)
                    .distinctBy { it.mac }
        }

        // Deprecated but still called on older devices
        @Deprecated("Deprecated in Android 13")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendCommand(cmd: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.e("BLE", "❌ Permission BLUETOOTH_CONNECT manquante!")
                return
            }
        }

        if (!isConnected) {
            Log.e("BLE", "❌ Not connected!")
            return
        }

        val service = gatt?.getService(SERVICE_UUID)
        if (service == null) {
            Log.e("BLE", "❌ Service not found!")
            return
        }

        val cmdChar = service.getCharacteristic(CMD_UUID)
        if (cmdChar == null) {
            Log.e("BLE", "❌ CMD characteristic not found!")
            return
        }

        // Use Android 13+ API or fallback
        val data = cmd.toByteArray()
        val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(cmdChar, data, writeType)
        } else {
            @Suppress("DEPRECATION")
            cmdChar.value = data
            @Suppress("DEPRECATION")
            cmdChar.writeType = writeType
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(cmdChar)
        }

        Log.d("BLE", "📤 CMD sent: $cmd")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                gatt?.disconnect()
                gatt?.close()
            }
        } else {
            gatt?.disconnect()
            gatt?.close()
        }
        gatt = null
        isConnected = false
    }

    fun clearMacDiplayed() {
        _macEvents.value = emptyList()
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
