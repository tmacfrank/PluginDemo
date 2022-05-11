package com.demo.hook.plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

public class LoadUtil {

    private static final String TAG = LoadUtil.class.getSimpleName();

    private static volatile Resources sResources;

    public static Resources getResources(Context context, String pluginPath) {
        if (sResources == null) {
            synchronized (LoadUtil.class) {
                if (sResources == null) {
                    sResources = loadResource(context, pluginPath);
                }
            }
        }
        return sResources;
    }

    public static Resources loadResource(Context context, String pluginPath) {
        if (TextUtils.isEmpty(pluginPath) || !new File(pluginPath).exists()) {
            Log.e(TAG, "插件包路径有误！");
            return null;
        }
        try {
            // 获取 AssetManager 并执行 addAssetPath() 将插件路径传递进去
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPathMethod.invoke(assetManager, pluginPath);

            // 创建一个绑定 assetManager 的 Resources 对象并返回，注意这个 context 不能是 Activity
            Resources resources = context.getResources();
            return new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
