/**
 * NoLauncher
 * Created by Saranomy on 2020-04-29.
 * Under Apache License Version 2.0, http://www.apache.org/licenses/
 * saranomy@gmail.com
 */
package com.saranomy.nolauncher;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppChangeReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context c, Intent intent) {
        if (MainActivity.self != null)
            MainActivity.self.loadApps();
    }
}
