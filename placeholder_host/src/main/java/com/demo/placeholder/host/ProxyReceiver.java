package com.demo.placeholder.host;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.demo.placeholder.lib.ReceiverInterface;

public class ProxyReceiver extends BroadcastReceiver {

    private String className;

    public ProxyReceiver(String className) {
        this.className = className;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Class<?> clazz = context.getClassLoader().loadClass(className);
            ReceiverInterface receiverInterface = (ReceiverInterface) clazz.newInstance();
            receiverInterface.onReceive(context, intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
