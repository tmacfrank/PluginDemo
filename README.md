# PluginDemo

A demo for Android plug-in.There are two ways to achieve the function:

1.**Placeholder**:refer to module placeholder_host,placeholder_plugin and placeholder_lib

2.**Hook**:refer to module hook_host,hook_plugin

Hook can load plugin class by adding dexElements to PathClassLoader,or adding LoadedApk to mPackages in ActivityThread.
