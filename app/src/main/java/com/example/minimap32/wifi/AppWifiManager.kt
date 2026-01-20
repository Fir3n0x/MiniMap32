package com.example.minimap32.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission
import com.example.minimap32.model.WifiNetwork


class AppWifiManager(
    private val context: Context
) {

    @SuppressLint("ServiceCast")
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var onResults: ((List<WifiNetwork>) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(
                WifiManager.EXTRA_RESULTS_UPDATED,
                false
            )

            val results = if (success) {
                wifiManager.scanResults.map {
                    WifiNetwork(
                        ssid = it.SSID,
                        bssid = it.BSSID,
                        level = it.level,
                        frequency = it.frequency
                    )
                }
            } else {
                emptyList()
            }

            onResults?.invoke(results)
        }
    }

    fun startScan(onResults: (List<WifiNetwork>) -> Unit) {
        this.onResults = onResults

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, filter)

        val success = wifiManager.startScan()
        if(!success) {
            onResults(emptyList())
        }
    }

    // To avoid memory loss
    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch(_: Exception) {}
    }

}