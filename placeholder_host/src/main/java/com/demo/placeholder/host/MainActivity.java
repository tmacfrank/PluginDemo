package com.demo.placeholder.host;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_goto_plugin).setOnClickListener(listener -> startPlugin());

        findViewById(R.id.btn_test_provider).setOnClickListener(listener -> testPluginProvider());
    }

    private void testPluginProvider() {
        ArrayList<Uri> list = new ArrayList<>();
        list.add(Uri.parse("content://com.demo.placeholder.host.ProxyProvider/plugin00"));
        list.add(Uri.parse("content://com.demo.placeholder.host.ProxyProvider/plugin01"));

        ContentResolver contentResolver = getContentResolver();
        for (Uri uri : list) {
            contentResolver.query(uri, null, null, null, null);
            contentResolver.insert(uri, new ContentValues());
            contentResolver.delete(uri, "", new String[]{});
            contentResolver.update(uri, new ContentValues(), "", new String[]{});
        }
    }

    private void startPlugin() {
        // 待加载插件路径
        String pluginPath = getFilesDir().getAbsolutePath() + File.separator + "placeholder_plugin-debug.apk";

        // 获取插件的 PackageInfo
        PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(pluginPath, PackageManager.GET_ACTIVITIES);
        if (packageInfo != null) {
            // 需要插件入口 Activity 在 AndroidManifest 中第一个定义
            ActivityInfo activityInfo = packageInfo.activities[0];
            Intent intent = new Intent(this, ProxyActivity.class);
            intent.putExtra("className", activityInfo.name);
            startActivity(intent);
        }
    }
}