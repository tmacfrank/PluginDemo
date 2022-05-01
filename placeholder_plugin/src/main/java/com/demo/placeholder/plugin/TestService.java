package com.demo.placeholder.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.demo.placeholder.lib.ServiceInterface;

public class TestService extends Service implements ServiceInterface {

    private static final String TAG = TestService.class.getSimpleName();

    public TestService() {
    }

    @Override
    public void insertAppContext(Service service) {

    }

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onCreate: TestService");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}