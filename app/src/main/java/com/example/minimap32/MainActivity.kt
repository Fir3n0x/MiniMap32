package com.example.minimap32

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.minimap32.presentation.BleScreen
import com.example.minimap32.ui.theme.MiniMap32Theme

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter

    // Request MULTIPLE permissions at once
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            android.util.Log.d("MainActivity", "📋 Permission results: $permissions")
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                android.util.Log.d("MainActivity", "✅ All permissions granted!")
                initBluetooth()
            } else {
                android.util.Log.e("MainActivity", "❌ Some permissions denied!")
                permissions.forEach { (perm, granted) ->
                    android.util.Log.e("MainActivity", "  $perm: $granted")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiniMap32Theme{
                BleScreen()
            }
        }

        // Request permissions for Android 12+ (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestBluetoothPermissions.launch(permissionsToRequest.toTypedArray())
            } else {
                initBluetooth()
            }
        } else {
            // For older Android versions, request location permission
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermissions.launch(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                )
            } else {
                initBluetooth()
            }
        }
    }

    private fun initBluetooth() {
        val bluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent =
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }
    }
}