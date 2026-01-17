package com.example.minimap32.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.minimap32.ble.BleManager

class BleViewModel(application: Application) : AndroidViewModel(application) {

    val bleManager = BleManager(application)
    val macEvents = bleManager.macEvents
}