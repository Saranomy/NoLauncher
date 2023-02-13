package com.saranomy.nolauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        NoLauncher.self?.load(intent?.action)
    }
}
