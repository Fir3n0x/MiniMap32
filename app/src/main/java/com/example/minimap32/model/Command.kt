package com.example.minimap32.model

sealed class Command {

    abstract fun toPayload(): String

    // SNIFFER
    data class SendSniffStart(
        val ssid: String,
        val bssid: String,
        val channel: Int
    ) : Command() {
        override fun toPayload(): String {
            return "SNIFF|START|SSID=$ssid|BSSID=$bssid|CHANNEL=$channel"
        }
    }

    object SendSniffStop : Command() {
        override fun toPayload(): String {
            return "SNIFF|STOP"
        }
    }



    // DEAUTH
    data class SendStartDeauth(
        val targetMac: String,
        val apMac: String,
        val channel: Int
    ) : Command() {
        override fun toPayload(): String {
            return "DEAUTH|START|TARGET=$targetMac|AP=$apMac|CHANNEL=$channel"
        }
    }

    object SendStopDeauth : Command() {
        override fun toPayload(): String {
            return "DEAUTH|STOP"
        }
    }

    // BEACON
    data class SendStartBeacon(
        val ssid: String,
        val channel: Int
    ) : Command() {
        override fun toPayload(): String {
            return "BEACON|START|SSID=$ssid|CHANNEL=$channel"
        }
    }
}