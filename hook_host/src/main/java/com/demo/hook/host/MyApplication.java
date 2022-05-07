package com.demo.hook.host;

import android.app.Application;

import java.io.File;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PluginManager pluginManager = PluginManager.getInstance(this);
        String pluginPath = getFilesDir() + File.separator + "hook_plugin-debug.apk";
        pluginManager.loadPlugin(pluginPath);
        pluginManager.hookAMS();
        pluginManager.hookHandler();
    }
}
