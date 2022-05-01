package com.demo.placeholder.host;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.demo.placeholder.host.manager.PluginManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_goto_plugin).setOnClickListener(listener -> {
            startPlugin();
        });
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