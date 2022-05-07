package com.demo.hook.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class PluginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        Log.d("Frank", "onCreate: PluginActivity");
    }
}