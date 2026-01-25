package com.example.minimap32.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimap32.ble.BleConnectionState
import com.example.minimap32.ble.BleManager
import com.example.minimap32.model.Command
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleViewModel(application: Application) : AndroidViewModel(application) {

    // BleManager
    val bleManager = BleManager(application)
    val macEvents = bleManager.macEvents
    val attackLogs = bleManager.attackLogs
    val statusEvents = bleManager.statusEvents

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

    // Handle snackbar Ble state
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            bleManager.connectionEvents.collect { state ->
                _connectionState.value = state

                if (state is BleConnectionState.Disconnected) {
                    _uiEvents.emit("ESP32 disconnected")
                }

                if (state is BleConnectionState.Error) {
                    _uiEvents.emit("BLE error: ${state.reason}")
                }

                if (state is BleConnectionState.Ready) {
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

    fun handleBleDisconnect() {
        viewModelScope.launch {

            // Stop ESP32 attacks
            bleManager.sendCommand(Command.SendSniffStop)
            bleManager.sendCommand(Command.SendStopDeauth)

            delay(300)

            // Reset ESP32 state
            bleManager.sendCommand(Command.SendClearMac)
            bleManager.sendCommand(Command.SendClearWifi)

            // Clear local state
            bleManager.clearMacDisplayed()
            bleManager.clearSnifferLogs()

            _selectedDevice.value = null
            _connectionState.value = BleConnectionState.Idle

            bleManager.resetSession()
        }
    }


    fun logLocal(msg: String) {
        bleManager.pushLocalLog(msg)
    }

    fun clearMacEvents() {
        bleManager.clearMacDisplayed()
    }

    fun clearSelection() {
        _selectedDevice.value = null
    }

    fun clearSnifferLogs() {
        bleManager.clearSnifferLogs()
    }

    fun notifyEsp32ClearMacs() {
        // Send message to clear mac esp32 side
        bleManager.sendCommand(Command.SendClearMac)
    }

    fun notifyEsp32ResetWifiVariables() {
        // Send message to clear mac esp32 side
        Log.d("BLE", "Sending WIFI CLEAR")
        bleManager.sendCommand(Command.SendClearWifi)
    }
}