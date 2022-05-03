package com.demo.placeholder.plugin;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

public class PluginActivity extends BaseActivity {

    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        // 启动插件 Activity
        findViewById(R.id.button_activity).setOnClickListener((listener) -> {
            Intent intent = new Intent(hostActivity, TestActivity.class);
            startActivity(intent);
        });

        // 启动插件 Service
        findViewById(R.id.button_service).setOnClickListener((listener) ->
                startService(new Intent(hostActivity, TestService.class))
        );

        // 注册插件动态广播
        findViewById(R.id.button_register_dynamic_broadcast).setOnClickListener((listener) -> {
            IntentFilter dynamicFilter = new IntentFilter();
            dynamicFilter.addAction("com.apk.plugin.receiver.dynamic");
            mReceiver = new TestReceiver();
            registerReceiver(mReceiver, dynamicFilter);
        });

        // 发送插件动态广播
        findViewById(R.id.button_send_dynamic_broadcast).setOnClickListener((listener) -> {
            Intent intent = new Intent();
            intent.setAction("com.apk.plugin.receiver.dynamic");
            sendBroadcast(intent);
        });

        // 发送插件静态广播
        findViewById(R.id.button_send_static_broadcast).setOnClickListener((listener) -> {
            Intent intent = new Intent();
            intent.setAction("com.demo.placeholder.plugin.test");
            sendBroadcast(intent);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}