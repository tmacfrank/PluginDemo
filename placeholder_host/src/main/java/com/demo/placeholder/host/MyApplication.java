package com.demo.placeholder.host;

import android.app.Application;
import android.content.res.Resources;
import android.util.Log;

import com.demo.placeholder.host.manager.PluginManager;

import java.io.File;
import java.sql.Array;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

public class MyApplication extends Application {

    private DexClassLoader mDexClassLoader;

    @Override
    public void onCreate() {
        super.onCreate();

        ArrayList<String> pluginPaths = new ArrayList<>();
        pluginPaths.add(getFilesDir().getAbsolutePath() + File.separator + "placeholder_plugin-debug.apk");
        mDexClassLoader = PluginManager.getInstance().loadPlugins(this, pluginPaths);
        PluginManager.getInstance().loadResources(this, pluginPaths);
        // 解析插件 apk，注册其中的静态 BroadcastReceiver
        PluginManager.getInstance().parseApk(this, pluginPaths);
    }

    @Override
    public ClassLoader getClassLoader() {
        return mDexClassLoader == null ? super.getClassLoader() : mDexClassLoader;
    }
}
