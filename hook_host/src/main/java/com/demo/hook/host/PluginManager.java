package com.demo.hook.host;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.ref.WeakReference;
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

    /**
     * BaseDexClassLoader 自己加载一个类的过程：
     * ClassLoader.loadClass() -> BaseDexClassLoader.findClass() ->
     * DexPathList.findClass() -> 遍历 Element[] dexElements 执行
     * Element.findClass() 最终调用到 native 方法去寻找类
     * <p>
     * 所以我们的思路是将插件的 dexElements 与宿主的 dexElements 合并形成
     * 一个新的 dexElements，在设置给宿主，这样宿主再通过 ClassLoader 加载
     * 时就可以加载插件中的类了。
     */
    public void loadPluginForHook(String pluginPath) {
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

    // 多 DexClassLoader 方式，每个插件有一个对应的 DexClassLoader 对象
    public ClassLoader loadPluginForLoadedApk(String pluginPath) {
        if (TextUtils.isEmpty(pluginPath)) {
            throw new IllegalArgumentException("插件路径不能拿为空！");
        }

        File pluginFile = new File(pluginPath);
        if (!pluginFile.exists()) {
            throw new IllegalArgumentException("插件包不存在！");
        }

        // 指定一个 optimizedDirectory 用以获取加载插件的 ClassLoader
        File optDir = mContext.getDir("optDir", Context.MODE_PRIVATE);
        return new DexClassLoader(pluginPath, optDir.getAbsolutePath(), null, mContext.getClassLoader());
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

    private final Handler.Callback mCallback = new Handler.Callback() {
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

                        // 如果使用 LoadedApk 方式加载插件，需要给 ActivityInfo.applicationInfo 做包名区分
                        if (BuildConfig.useLoadedApk) {
                            Field activityInfoField = message.obj.getClass().getDeclaredField("activityInfo");
                            activityInfoField.setAccessible(true);
                            ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(message.obj);

                            if (activityInfo != null) {
                                // actionIntent.getPackage() == null 说明是插件，就要取 Component 的包名
                                activityInfo.applicationInfo.packageName = actionIntent.getPackage() == null ?
                                        actionIntent.getComponent().getPackageName() : actionIntent.getPackage();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    };

    // LoadedApk start
    /**
     * 反射执行 PackageParser 的 generateApplicationInfo() 以获取 ApplicationInfo 对象
     */
    private ApplicationInfo getPluginAppInfo(File pluginFile) {
        try {
            // 1.调用 parsePackage() 得到 PackageParser.Package 对象
            Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");
            Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage",
                    File.class, int.class);
            Object packageParser = packageParserClass.newInstance();
            Object packageObject = parsePackageMethod.invoke(packageParser, pluginFile, PackageManager.GET_ACTIVITIES);

            // 2.创建一个 PackageUserState 对象
            Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
            Object packageUserState = packageUserStateClass.newInstance();

            // 3. 调用 generateApplicationInfo()
            Class<?> packageClass = Class.forName("android.content.pm.PackageParser$Package");
            Method generateApplicationInfoMethod = packageParserClass.getDeclaredMethod(
                    "generateApplicationInfo", packageClass, int.class, packageUserStateClass);
            ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(
                    null, packageObject, 0, packageUserState);

            // 4.指定 applicationInfo 中的路径，设置成插件的路径
            if (pluginFile != null) {
                String pluginApkPath = pluginFile.getAbsolutePath();
                applicationInfo.sourceDir = pluginApkPath;
                applicationInfo.publicSourceDir = pluginApkPath;
            }

            return applicationInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 自定义一个 LoadedApk，其 ClassLoader 可以加载插件类
     */
    public void makeCustomLoadedApk(File pluginFile) {
        try {
            // 1.获取 ActivityThread 对象
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            // 2.获取 ApplicationInfo
            ApplicationInfo pluginAppInfo = getPluginAppInfo(pluginFile);

            // 3.获取 CompatibilityInfo
            Class<?> compatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
            Field defaultField = compatibilityInfoClass.getField("DEFAULT_COMPATIBILITY_INFO");
            Object compatibilityInfo = defaultField.get(null);

            // 4.反射调用 getPackageInfoNoCheck()
            Method getPackageInfoNoCheckMethod = activityThreadClass.getMethod("getPackageInfoNoCheck",
                    ApplicationInfo.class, compatibilityInfoClass);
            Object loadedApk = getPackageInfoNoCheckMethod.invoke(activityThread, pluginAppInfo, compatibilityInfo);

            // 5.修改 loadedApk 中的 ClassLoader，替换成可以加载插件中类的 ClassLoader
            ClassLoader classLoader = loadPluginForLoadedApk(pluginFile.getAbsolutePath());
            Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
            Field mClassLoaderField = loadedApkClass.getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            mClassLoaderField.set(loadedApk, classLoader);

            // 6.将 loadedApk 存入 ActivityThread 的缓存 mPackages 中
            Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
            mPackagesField.setAccessible(true);
            ArrayMap mPackages = (ArrayMap) mPackagesField.get(activityThread);

            // LoadedApk 是 @hide 的，所以 ArrayMap、WeakReference 无法指定泛型
            WeakReference weakReference = new WeakReference(loadedApk);
            mPackages.put(pluginAppInfo.packageName, weakReference);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hookGetPackageInfo() {
        try {
            // 反射获取 ActivityThread 内的静态变量 sPackageManager
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManager = sPackageManagerField.get(null);

            Class<?> iPackageManagerClass = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                    new Class[]{iPackageManagerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            //
                            if ("getPackageInfo".equals(method.getName())) {
                                return new PackageInfo();
                            }
                            return method.invoke(sPackageManager, args);
                        }
                    });

            sPackageManagerField.set(null, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // LoadedApk end
}
