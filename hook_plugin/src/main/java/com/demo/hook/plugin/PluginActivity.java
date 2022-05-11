package com.demo.hook.plugin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public class PluginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_plugin, null);
        setContentView(view);
    }
}