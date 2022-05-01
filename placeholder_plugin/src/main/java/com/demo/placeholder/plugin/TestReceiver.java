package com.demo.placeholder.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.demo.placeholder.lib.ReceiverInterface;

public class TestReceiver extends BroadcastReceiver implements ReceiverInterface {

    private static final String TAG = TestReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);
    }
}