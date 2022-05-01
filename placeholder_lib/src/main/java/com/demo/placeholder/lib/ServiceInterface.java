package com.demo.placeholder.lib;

import android.app.Service;
import android.content.Intent;

public interface ServiceInterface {

    void insertAppContext(Service service);

    void onCreate();

    int onStartCommand(Intent intent, int flags, int startId);

    void onDestroy();
}
