package com.example.minimap32.model

// Format TYPE|key=value|key=value|...
sealed class BleEvent {
    data class Log(val message: String) : BleEvent()
    data class MacFound(val mac: String, val rssi: Int, val channel: Int) : BleEvent()
    data class Status(val value: String) : BleEvent()
    data class Error(val message: String) : BleEvent()
    data class Unknown(val raw: String) : BleEvent()
}