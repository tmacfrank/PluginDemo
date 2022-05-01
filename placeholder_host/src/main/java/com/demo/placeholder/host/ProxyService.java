package com.demo.placeholder.host;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;

import com.demo.placeholder.host.manager.PluginManager;
import com.demo.placeholder.lib.ServiceInterface;

public class ProxyService extends Service {

    public ProxyService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String serviceName = intent.getStringExtra("className");

        try {
            Class<?> clazz = getClassLoader().loadClass(serviceName);
            ServiceInterface serviceInterface = (ServiceInterface) clazz.newInstance();
            serviceInterface.insertAppContext(this);
            serviceInterface.onStartCommand(intent, flags, startId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}