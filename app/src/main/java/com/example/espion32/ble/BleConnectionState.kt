package com.example.espion32.ble

sealed class BleConnectionState() {
    object Idle : BleConnectionState()
    object Connecting : BleConnectionState()
    object Connected : BleConnectionState()
    object Disconnected : BleConnectionState()
    object MtuRequested : BleConnectionState()
    object ServicesDiscovered : BleConnectionState()
    object Ready : BleConnectionState()
    data class Error(val reason: String) : BleConnectionState()
}
