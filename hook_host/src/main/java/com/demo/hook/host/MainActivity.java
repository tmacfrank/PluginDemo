package com.demo.hook.host;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startPluginActivity(View view) {
        Intent intent = new Intent();
        String pluginPackage = "com.demo.hook.plugin";
        String pluginActivityClass = "com.demo.hook.plugin.PluginActivity";
        intent.setComponent(new ComponentName(pluginPackage, pluginActivityClass));
        startActivity(intent);
    }
}