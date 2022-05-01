package com.demo.placeholder.plugin;

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
    private static String mCurrentPluginPath;

    public static Resources getResources(Context context) {
        if (sResources == null) {
            synchronized (LoadUtil.class) {
                if (sResources == null) {
                    mCurrentPluginPath = context.getFilesDir().getAbsolutePath() +
                            File.separator + "placeholder_plugin-debug.apk";
                    sResources = loadResource(context);
                }
            }
        }
        return sResources;
    }

    public static Resources loadResource(Context context) {
        if (TextUtils.isEmpty(mCurrentPluginPath) || !new File(mCurrentPluginPath).exists()) {
            Log.e(TAG, "插件包路径有误！");
            return null;
        }
        try {
            // 获取 AssetManager 并执行 addAssetPath() 将插件路径传递进去
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPathMethod.invoke(assetManager, mCurrentPluginPath);

            // 创建一个绑定 assetManager 的 Resources 对象并返回
            Resources resources = context.getResources();
            return new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
