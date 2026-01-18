package com.example.minimap32.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimap32.ble.BleConnectionState
import com.example.minimap32.ble.BleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleViewModel(application: Application) : AndroidViewModel(application) {

    // BleManager
    val bleManager = BleManager(application)
    val macEvents = bleManager.macEvents

    // BleConnection State
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val connectionState = _connectionState.asStateFlow()

    // Devices scan
    val devices = bleManager.devices.asStateFlow()

    // Device selected
    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    // Timeout job
    private var timeoutJob: Job? = null

    init {
        viewModelScope.launch {
            bleManager.connectionEvents.collect {
                _connectionState.value = it
                if (it is BleConnectionState.Ready) {
                    timeoutJob?.cancel()
                }
            }
        }
    }

    fun startScan() {
        // Clear previous scan results
        bleManager.devices.value = emptyList()
        bleManager.startScan()
    }

    fun selectDevice(device: BluetoothDevice) {
        bleManager.resetSession()
        _connectionState.value = BleConnectionState.Idle
        _selectedDevice.value = device
    }

    fun connectToSelectedDevice() {
        val device = selectedDevice.value ?: return

        // Reset before connection
        bleManager.resetSession()
        _connectionState.value = BleConnectionState.Idle

        _connectionState.value = BleConnectionState.Connecting
        bleManager.connect(device)

        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(6000) // 6 secondes
            if (connectionState.value !is BleConnectionState.Ready) {
                bleManager.resetSession()
                _connectionState.value = BleConnectionState.Error("Connection timeout")
            }
        }
    }

    fun clearSelection() {
        _selectedDevice.value = null
    }
}