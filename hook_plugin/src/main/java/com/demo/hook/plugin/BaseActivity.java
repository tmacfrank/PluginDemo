package com.demo.hook.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;

public class BaseActivity extends AppCompatActivity {

    private static final String PLUGIN_APK_PATH = "/data/data/com.demo.hook.host/files/hook_plugin-debug.apk";
    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = LoadUtil.getResources(getApplicationContext(), PLUGIN_APK_PATH);
        mContext = new ContextThemeWrapper(getBaseContext(), 0);
        // 替换 ContextImpl 中的 mResources
        Class<? extends Context> clazz = mContext.getClass();
        try {
            Field mResourcesField = clazz.getDeclaredField("mResources");
            mResourcesField.setAccessible(true);
            mResourcesField.set(mContext, resources);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Resources getResources() {
        /* 这种方式也不行，有冲突，改在 onCreate() 中实现
        // 插件没有 Application，所以 getApplicationContext()/getApplication() 拿到的都是宿主的
        Resources resources = LoadUtil.getResources(getApplicationContext(), PLUGIN_APK_PATH);
        // 插件作为单独 app 时需要返回 super.getResources()
        return resources == null ? super.getResources() : resources;*/
        return super.getResources();
    }
}
