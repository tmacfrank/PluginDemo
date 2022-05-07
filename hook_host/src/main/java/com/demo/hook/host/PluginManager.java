package com.demo.hook.host;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;

public class PluginManager {

    private static final String TAG = PluginManager.class.getSimpleName();
    private static final String PACKAGE_PLUGIN = "com.demo.hook.plugin";

    private static volatile PluginManager sPluginManager;
    private final Context mContext;

    private PluginManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static PluginManager getInstance(Context context) {
        if (sPluginManager == null) {
            synchronized (PluginManager.class) {
                if (sPluginManager == null) {
                    sPluginManager = new PluginManager(context);
                }
            }
        }
        return sPluginManager;
    }

    public void loadPlugin(String pluginPath) {
        if (TextUtils.isEmpty(pluginPath)) {
            Log.e(TAG, "插件路径不能拿为空！");
        }

        File pluginFile = new File(pluginPath);
        if (!pluginFile.exists()) {
            Log.e(TAG, "插件包不存在！");
        }

        try {
            // 1.获取宿主的 dexElements
            Class<?> clazz = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = clazz.getDeclaredField("pathList");
            pathListField.setAccessible(true);

            Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
            Field dexElements = dexPathListClass.getDeclaredField("dexElements");
            dexElements.setAccessible(true);

            ClassLoader pathClassLoader = mContext.getClassLoader();
            Object dexPathList = pathListField.get(pathClassLoader);
            Object[] hostElements = (Object[]) dexElements.get(dexPathList);

            // 2.获取插件的 dexElements
            DexClassLoader dexClassLoader = new DexClassLoader(pluginPath,
                    mContext.getCacheDir().getAbsolutePath(), null, pathClassLoader);
            Object pluginPathList = pathListField.get(dexClassLoader);
            Object[] pluginElements = (Object[]) dexElements.get(pluginPathList);

            // 3.合并到新的 Element[] 中，先创建一个新数组
            Object[] newElements = (Object[]) Array.newInstance(hostElements.getClass().getComponentType(),
                    hostElements.length + pluginElements.length);
            System.arraycopy(hostElements, 0, newElements, 0, hostElements.length);
            System.arraycopy(pluginElements, 0, newElements, hostElements.length, pluginElements.length);

            // 4.赋值
            dexElements.set(dexPathList, newElements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hookAMS() {
        try {
            // 1.反射调用 ActivityManager.getService() 得到 IActivityManager 对象
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Method getServiceMethod = activityManagerClass.getMethod("getService");
            Object iActivityManager = getServiceMethod.invoke(null);

            // 2.生成动态代理对象，监听 IActivityManager 方法执行，替换掉 startActivity() 的参数 intent
            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            Object iActivityManagerProxy = Proxy.newProxyInstance(mContext.getClassLoader(),
                    new Class[]{iActivityManagerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Log.d(TAG, "invoke: " + method.getName() + "/" + method.getDeclaringClass().getName());
                            // todo 通过包名过滤一下，只处理插件中的方法
                            if ("startActivity".equals(method.getName())) {
                                // startActivity() 有多个重载方法，参数位置不固定，所以先找一下 Intent
                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] instanceof Intent) {
                                        // 替换 Intent 为宿主的代理先
                                        Intent proxyIntent = new Intent(mContext, ProxyActivity.class);
                                        proxyIntent.putExtra("actionIntent", (Intent) args[i]);
                                        args[i] = proxyIntent;
                                        Log.d(TAG, "invoke: 替换了");
                                    }
                                }
                            }
                            // 不要改变原有的执行流程
                            return method.invoke(iActivityManager, args);
                        }
                    });

            // 3.用动态代理对象替换掉 IActivityManagerSingleton 内的 mInstance
            Field iSingletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
            iSingletonField.setAccessible(true);
            Object IActivityManagerSingleton = iSingletonField.get(null);

            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            mInstanceField.set(IActivityManagerSingleton, iActivityManagerProxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给 ActivityThread 中的 Handler——mH 的 mCallback 字段设置为
     * 我们自定义的 mCallback 对象
     */
    public void hookHandler() {
        try {
            // 1.通过 currentActivityThread() 拿到 ActivityThread 对象
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            // 2.拿到 mH 对象
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Object mH = mHField.get(activityThread);

            // 3.设置 mH 的 mCallback 字段
            Class<?> handlerClass = Class.forName("android.os.Handler");
            Field mCallbackField = handlerClass.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mH, mCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final int LAUNCH_ACTIVITY = 100;

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == LAUNCH_ACTIVITY) {
                try {
                    // message.obj 实际上是一个 ActivityClientRecord，但是 @hide 的，
                    // 我们不能直接用，只能一步直接拿 ActivityClientRecord 的 intent
                    Field intentField = message.obj.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);
                    Intent intent = (Intent) intentField.get(message.obj);

                    // 之前 Hook AMS 的时候我们把真正启动插件的 Intent 放在 Extra 里了
                    Intent actionIntent = intent.getParcelableExtra("actionIntent");
                    if (actionIntent != null) {
                        intentField.set(message.obj, actionIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    };
}
