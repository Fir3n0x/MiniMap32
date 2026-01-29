package com.example.espion32.model

// Format TYPE|SUBTYPE(SNIFF,DEAUTH,...)|key=value|key=value|...
sealed class BleEvent {
    abstract val subtype: String?

    data class Log(
        override val subtype: String?,
        val message: String
    ) : BleEvent()

    data class MacFound(
        override val subtype: String?,
        val mac: String,
        val rssi: Int,
        val channel: Int
    ) : BleEvent()

    data class Status(
        override val subtype: String?,
        val value: String
    ) : BleEvent()

    data class Error(
        override val subtype: String?,
        val message: String
    ) : BleEvent()

    data class Unknown(
        val raw: String
    ) : BleEvent() {
        override val subtype: String? = null
    }
}