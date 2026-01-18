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
import com.example.minimap32.model.BleDevice
import com.example.minimap32.model.MAC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BleManager(
    private val context: Context
) {

    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var gatt: BluetoothGatt? = null
    private var isConnected = false

    private val serviceUUID =
        UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    // Android -> ESP32 (WRITE)
    private val cmdUUID =
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    // ESP32 -> Android (NOTIFY)
    private val statusUUID =
        UUID.fromString("9d8c2d3a-7a12-4d3f-8f58-bc6b4f9c1123")

    // Descriptor
    private val cccdUUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Handle mac event
    private val _macEvents = MutableStateFlow<List<MAC>>(emptyList())
    val macEvents: StateFlow<List<MAC>> = _macEvents

    // Handle Discovered BLE devices
    val devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    // Handle BLE connection state
    val connectionEvents = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        // Check permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_SCAN
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                Log.e("BLE", "[W] Permission BLUETOOTH_SCAN missing!")
                return
            }
        } else {
            // Android < 12 requires LOCATION permission for BLE scan
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e("BLE", "[W] Permission ACCESS_FINE_LOCATION missing!")
                return
            }
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        Log.d("BLE", "[I] Scan started")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        Log.d("BLE", "[I] Scan stopped")
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

            if(devices.value.none { it.address == device.address}) {
                devices.value += device
            }

//            val bleDevice = BleDevice(
//                name = device.name ?: "Unknown",
//                mac = device.address
//            )
//
//            if(devices.value.none { it.mac == bleDevice.mac}) {
//                devices.value += bleDevice
//            }

//            if (deviceName == "Minimap32") {
//                // Only connect if signal is strong enough
//                if (result.rssi < -90) {
//                    Log.d("BLE", "[W] Signal too weak (${result.rssi}), waiting...")
//                    return
//                }
//
//                Log.d("BLE", "[I] ESP32 trouvé")
//                bluetoothAdapter.bluetoothLeScanner.stopScan(this)
//                connect(device)
//            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "[F] Scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.e("BLE", "[W] Permission BLUETOOTH_CONNECT missing!")
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
                    Log.d("BLE", "[I] Connecting to device...")

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

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLE", "[F] Connection failed: status=$status")
                connectionEvents.value =
                    BleConnectionState.Error("Connection failed (status=$status)")
                gatt.close()
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // Update ble connection state
                    connectionEvents.value = BleConnectionState.Connected

                    Log.d("BLE", "[I] Connected, requesting MTU...")
                    gatt.requestMtu(64)
                    Log.d("BLE", "[I] Connected, discovering services...")

                    isConnected = true
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // Update ble connection state
                    connectionEvents.value = BleConnectionState.Disconnected

                    Log.d("BLE", "[I] Disconnected")
                    isConnected = false
                    gatt.close()
                }
                else -> {
                    // Update ble connection state
                    connectionEvents.value = BleConnectionState.Error("Connection failed")

                    Log.d("BLE", "[F] Connection failed")
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Update ble connection state
                connectionEvents.value = BleConnectionState.MtuRequested

                Log.d("BLE", "[I] MTU negotiated: $mtu")
                gatt.discoverServices()
            } else {
                // Update ble connection state
                connectionEvents.value = BleConnectionState.Error("MTU failed")

                Log.e("BLE", "[F] MTU request failed")
            }
        }


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(serviceUUID)
                if (service == null) {
                    connectionEvents.value = BleConnectionState.Error("Service not found.")
                    Log.e("BLE", "[F] Service not found!")
                    return
                }

                // Update ble connection state
                connectionEvents.value = BleConnectionState.ServicesDiscovered

                Log.d("BLE", "[I] Services discovered!")

                val statusChar = service.getCharacteristic(statusUUID)
                if (statusChar == null) {
                    connectionEvents.value = BleConnectionState.Error("Status not found.")
                    Log.e("BLE", "[F] STATUS characteristic not found!")
                    return
                }

                // Enable notifications - USE NEW API
                gatt.setCharacteristicNotification(statusChar, true)

                val cccd = statusChar.getDescriptor(cccdUUID)
                if (cccd == null) {
                    connectionEvents.value = BleConnectionState.Error("CCCD descriptor not found.")
                    Log.e("BLE", "[F] CCCD descriptor not found!")
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

                Log.d("BLE", "[I] Notifications enabled!")

            } else {
                connectionEvents.value = BleConnectionState.Error("Service discovery failed")
                Log.e("BLE", "[F] Service discovery failed: $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "[I] Descriptor written successfully")

                // SET READY STATE HERE - after descriptor is confirmed written
                connectionEvents.value = BleConnectionState.Ready
            } else {
                Log.e("BLE", "[F] Descriptor write failed: $status")
                connectionEvents.value = BleConnectionState.Error("Descriptor write failed")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "[I] Characteristic written successfully")
            } else {
                Log.e("BLE", "[F] Characteristic write failed: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid != statusUUID) return

            val msg = value.toString(Charsets.UTF_8)
            Log.d("BLE", "[I] NOTIFY: $msg")

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
                Log.e("BLE", "[W] Permission BLUETOOTH_CONNECT missing!")
                return
            }
        }

        if (!isConnected) {
            Log.e("BLE", "[F] Not connected!")
            return
        }

        val service = gatt?.getService(serviceUUID)
        if (service == null) {
            Log.e("BLE", "[F] Service not found!")
            return
        }

        val cmdChar = service.getCharacteristic(cmdUUID)
        if (cmdChar == null) {
            Log.e("BLE", "[F] CMD characteristic not found!")
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

        Log.d("BLE", "[I] CMD sent: $cmd")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        // Disconnect GATT
        gatt?.disconnect()
        gatt?.close()
        gatt = null

        // Reset internal flags
        isConnected = false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun resetSession() {
        Log.d("BLE", "[I] Reset BLE session")

        // disconnect
        disconnect()

        // RESET ALL STATES
        connectionEvents.value = BleConnectionState.Idle
        _macEvents.value = emptyList()
        devices.value = emptyList()
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
