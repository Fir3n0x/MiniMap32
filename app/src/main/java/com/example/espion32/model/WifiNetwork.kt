package com.example.espion32.model

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val frequency: Int,
    val channel: Int
)

fun WifiNetwork.displayName(): String {
    return "${ssid.ifBlank { "<hidden>" }} (${frequency} MHz)"
}
