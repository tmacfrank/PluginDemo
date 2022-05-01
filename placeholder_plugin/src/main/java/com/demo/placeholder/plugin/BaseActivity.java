package com.demo.placeholder.plugin;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.demo.placeholder.lib.ActivityInterface;

import java.io.File;
import java.lang.reflect.Field;

public class BaseActivity extends AppCompatActivity implements ActivityInterface {

    protected Activity hostActivity;

    @Override
    public void insertAppContext(Activity activity) {
        hostActivity = activity;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(Bundle savedInstanceState) {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onStart() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onResume() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onPause() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onStop() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onDestroy() {

    }

    @Override
    public void setContentView(int layoutResID) {
        if (hostActivity != null) {
            hostActivity.setContentView(layoutResID);
        }
    }

    @Override
    public <T extends View> T findViewById(int id) {
        if (hostActivity != null) {
            return hostActivity.findViewById(id);
        }
        // super 其实是用不了的
        return super.findViewById(id);
    }

    @Override
    public void startActivity(Intent intent) {
        Intent newIntent = new Intent();
        newIntent.putExtra("className", intent.getComponent().getClassName());
        hostActivity.startActivity(newIntent);
    }

    @Override
    public ComponentName startService(Intent service) {
        Intent intent = new Intent();
        intent.putExtra("className", service.getComponent().getClassName());
        return hostActivity.startService(intent);
    }
}