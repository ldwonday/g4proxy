package com.virjar.g4proxy.android;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by virjar on 2018/8/23.
 */
public class StartOnBootBroadcastReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i("weijia", "receive start service broadcast");
        context.startService(new Intent(context, HttpProxyService.class));


        // clearAbortBroadcast();
    }
}
