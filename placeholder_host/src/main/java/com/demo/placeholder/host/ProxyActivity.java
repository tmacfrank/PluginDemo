package com.demo.placeholder.host;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.demo.placeholder.host.manager.PluginManager;
import com.demo.placeholder.lib.ActivityInterface;

import java.io.File;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

public class ProxyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String target = getIntent().getStringExtra("className");
        try {
            Class<?> targetClass = getClassLoader().loadClass(target);
            ActivityInterface activityInterface = (ActivityInterface) targetClass.newInstance();
            // 给插件注入宿主环境
            activityInterface.insertAppContext(this);
            // 调用插件 Activity 的 onCreate()
            activityInterface.onCreate(new Bundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startActivity(Intent intent) {
        String className = intent.getStringExtra("className");
        Intent proxyIntent = new Intent(this, ProxyActivity.class);
        proxyIntent.putExtra("className", className);
        super.startActivity(proxyIntent);
    }

    @Override
    public ComponentName startService(Intent service) {
        Intent intent = new Intent(this, ProxyService.class);
        intent.putExtra("className", service.getStringExtra("className"));
        return super.startService(intent);
    }

    @Override
    public Resources getResources() {
        Resources resources = PluginManager.getInstance().getResources();
        return resources == null ? super.getResources() : resources;
    }
}