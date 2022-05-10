package com.demo.hook.host;

import android.app.Application;

import java.io.File;

public class MyApplication extends Application {

    private boolean useLoadedApk = true;

    @Override
    public void onCreate() {
        super.onCreate();

        PluginManager pluginManager = PluginManager.getInstance(this);
        String pluginPath = getFilesDir() + File.separator + "hook_plugin-debug.apk";

        pluginManager.hookAMS();
        pluginManager.hookHandler();
        if (useLoadedApk) {
            // 使用 LoadedApk 式的 ClassLoader 加载插件
            pluginManager.makeCustomLoadedApk(new File(pluginPath));
            pluginManager.hookGetPackageInfo();
        } else {
            // 使用 Hook 式加载插件类
            pluginManager.loadPluginForHook(pluginPath);
        }
    }
}
