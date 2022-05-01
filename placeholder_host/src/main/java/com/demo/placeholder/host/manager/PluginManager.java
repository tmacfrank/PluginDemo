package com.demo.placeholder.host.manager;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.DexClassLoader;

public class PluginManager {

    private static final String TAG = PluginManager.class.getSimpleName();

    private static volatile PluginManager sManager;

    private Resources mResources;

    private PluginManager() {
    }

    public static PluginManager getInstance() {
        if (sManager == null) {
            synchronized (PluginManager.class) {
                if (sManager == null) {
                    sManager = new PluginManager();
                }
            }
        }
        return sManager;
    }

    // 单 DexClassLoader 方式，将所有插件的加载任务都放在一个 DexClassLoader 中
    public DexClassLoader loadPlugins(Context context, List<String> pluginPaths) {
        if (pluginPaths == null || pluginPaths.isEmpty()) {
            Log.e(TAG, "插件集合为空！！！");
            return null;
        }

        try {
            // 1.获取宿主的 dexElements
            Class<?> clazz = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = clazz.getDeclaredField("pathList");
            pathListField.setAccessible(true);

            Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
            Field dexElements = dexPathListClass.getDeclaredField("dexElements");
            dexElements.setAccessible(true);

            ClassLoader pathClassLoader = context.getClassLoader();
            Object dexPathList = pathListField.get(pathClassLoader);
            Object[] hostElements = (Object[]) dexElements.get(dexPathList);

            DexClassLoader dexClassLoader = null;
            for (String pluginPath : pluginPaths) {
                // 2.获取插件的 dexElements
                dexClassLoader = new DexClassLoader(pluginPath,
                        context.getCacheDir().getAbsolutePath(), null, pathClassLoader);
                Object pluginPathList = pathListField.get(dexClassLoader);
                Object[] pluginElements = (Object[]) dexElements.get(pluginPathList);

                // 3.合并到新的 Element[] 中，先创建一个新数组
                Object[] newElements = (Object[]) Array.newInstance(hostElements.getClass().getComponentType(),
                        hostElements.length + pluginElements.length);
                System.arraycopy(hostElements, 0, newElements, 0, hostElements.length);
                System.arraycopy(pluginElements, 0, newElements, hostElements.length, pluginElements.length);

                // 4.赋值
                dexElements.set(dexPathList, newElements);
            }
            return dexClassLoader;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void loadResources(Context context, List<String> pluginPaths) {
        if (pluginPaths == null || pluginPaths.size() == 0) {
            throw new IllegalArgumentException("插件集合不能拿为空！");
        }

        try {
            // 获取 AssetManager 并执行 addAssetPath() 将插件路径传递进去
            /**
             *  注意与 AssetManager.class.newInstance() 的区别，通过 context 获取的是负责加载宿主资源的
             *  AssetManager 对象，而 newInstance() 是新创建一个，就看 mResources 是需要加载宿主与插件的
             *  资源，还是只加载插件中的资源，显然我们这里是需要 mResources 既加载宿主，又加载插件。
             */
            AssetManager assetManager = context.getAssets();
            Method addAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
            for (String pluginPath : pluginPaths) {
                File pluginFile = new File(pluginPath);
                if (!pluginFile.exists()) {
                    Log.e(TAG, "插件文件不存在：" + pluginPath);
                    continue;
                }
                addAssetPathMethod.invoke(assetManager, pluginPath);
            }
            // 创建一个绑定 assetManager 的 Resources 对象并返回
            Resources resources = context.getResources();
            mResources = new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
            Log.d(TAG, "loadResources: "+mResources);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Resources getResources() {
        return mResources;
    }
}
