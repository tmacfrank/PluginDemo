package com.demo.placeholder.host;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyProvider extends ContentProvider {

    private static final String TAG = ProxyProvider.class.getSimpleName();

    // Uri path 与插件的 ContentProvider 全类名的映射关系表
    private static final Map<String, String> sClassNameMap;
    // 插件的 ContentProvider 全类名与实例对象的缓存
    private Map<String, ContentProvider> mCache;
    private ContentProvider mPluginProvider;

    static {
        sClassNameMap = new HashMap<>();
        sClassNameMap.put("plugin00", "com.demo.placeholder.plugin.PluginProvider");
        sClassNameMap.put("plugin01", "com.demo.placeholder.plugin.PluginProvider1");
    }

    /**
     * 1.根据要操作的 Uri 先找出对应的插件 ContentProvider 的全类名
     * 2.利用全类名反射得到插件 ContentProvider 的实例对象
     * 后续 CURD 操作都需要先来拿 ContentProvider 实例再操作
     */
    public ContentProvider loadPluginProvider(Uri uri) {
        Log.d(TAG, "Uri: " + uri.toString());
        // uri.getPath() 拿到的是 /plugin00，而 getPathSegments() 拿到的是
        // 没有 / 的，每段路径名组成的集合，对于当前 demo 的代码直接取 pathSegments(0)
        List<String> pathSegments = uri.getPathSegments();
        String className = sClassNameMap.get(pathSegments.get(0));

        // 先去缓存中找
        if (mCache != null) {
            ContentProvider contentProvider = mCache.get(className);
            if (contentProvider != null) {
                return contentProvider;
            }
        }

        // 缓存没有再反射
        try {
            Class<?> clazz = getContext().getClassLoader().loadClass(className);
            mPluginProvider = (ContentProvider) clazz.newInstance();
            if (mCache == null) {
                mCache = new HashMap<>();
            }
            mCache.put(className, mPluginProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "loadPluginProvider: " + mPluginProvider);
        return mPluginProvider;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        ContentProvider pluginProvider = loadPluginProvider(uri);
        if (pluginProvider != null) {
            return pluginProvider.delete(uri, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        ContentProvider pluginProvider = loadPluginProvider(uri);
        if (pluginProvider != null) {
            return pluginProvider.getType(uri);
        }
        return "";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        ContentProvider pluginProvider = loadPluginProvider(uri);
        if (pluginProvider != null) {
            return pluginProvider.insert(uri, values);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate: ");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        ContentProvider pluginProvider = loadPluginProvider(uri);
        if (pluginProvider != null) {
            return pluginProvider.query(uri, projection, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        ContentProvider pluginProvider = loadPluginProvider(uri);
        if (pluginProvider != null) {
            pluginProvider.update(uri, values, selection, selectionArgs);
        }
        return 0;
    }
}