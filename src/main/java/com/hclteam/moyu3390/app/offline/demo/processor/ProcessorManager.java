/**
 * ProcessorManager
 * <p> 应用事务管理-方式1，多版本替换下线采用临时容器存储新应用的方式
 * 1.0
 * <p>
 * 2023/2/1 16:45
 */

package com.hclteam.moyu3390.app.offline.demo.processor;

import com.hclteam.moyu3390.app.offline.demo.loaders.AppClassLoader;
import com.hclteam.moyu3390.app.offline.demo.loaders.ClassScanner;
import com.hclteam.moyu3390.app.offline.demo.loaders.JarClassLoader;
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class ProcessorManager {
    private static volatile Map<String, AppInfo> appProcessorMap = new ConcurrentHashMap<>();
    private static volatile Map<String, AppInfo> appProcessorTempMap = new ConcurrentHashMap<>();

    public static final String jarDir = "D:\\test\\lib";

    public static final String packagePath = "cn.com.infosec.mock.app.processor";
    static Object lock = new Object();

    /**
     * 根据应用标识和事务标识查找对应的事务处理对象 --采用新版本应用放到临时容器的方式
     *
     * @param appId
     * @param processorId
     * @return
     */
    public static Object getProcessor(String appId, String processorId) {

        AppInfo appInfo = getAppInfo(appId);
        // 判断应用状态是否下线
        if (Objects.isNull(appInfo) || appInfo.isOffline) {
            // 下线状态 从临时容器中查找应用信息
            appInfo = appProcessorTempMap.get(appId);
            if (Objects.isNull(appInfo)) {
                return null;
            }
        }
        Map<String, Object> processorMap = appInfo.getProcessors();
        if (CollectionUtils.isEmpty(processorMap)) {
            return null;
        }
        Object processor = processorMap.get(processorId);
        return processor;
    }

    /**
     * 查看是否存在同一标识的应用
     *
     * @param appId
     * @return
     */
    public static synchronized boolean isRunning(String appId) {
        AppInfo appInfo = getAppInfo(appId);
        return Objects.nonNull(appInfo);
    }

    public static AppInfo getAppInfo(String appId) {
        AppInfo appInfo = appProcessorMap.get(appId);
        return appInfo;
    }

    public static boolean setAppInfoIfAbsent(AppInfo appInfo) {
        // map中不存在才能put成功
        AppInfo appInfo1 = appProcessorMap.putIfAbsent(appInfo.getAppId(), appInfo);
        return Objects.isNull(appInfo1);
    }


    /**
     * 获取指定的下线的应用信息
     *
     * @param appId
     * @return
     */
    public static AppInfo getOfflineApp(String appId) {
        return getAppInfo(appId);
    }

    /**
     * 从正式容器中移除指定的应用信息 --采用新版本应用放到临时容器的方式
     *
     * @param appId
     * @return
     */
    public static boolean removeApp(String appId) {
        AppInfo remove = appProcessorMap.remove(appId);
        return true;
    }

    public static boolean setApp2TempMap(AppInfo appInfo) {
        appProcessorTempMap.put(appInfo.getAppId(),appInfo);
        return true;
    }

    /**
     * 从临时容器置换到正式容器中 --采用新版本应用放到临时容器的方式
     *
     * @param appId
     * @return
     */
    public static boolean unifyAppInfo(String appId) {
        // 从临时容器中查找到对象，并set到正式容器中
        AppInfo appInfo = appProcessorTempMap.get(appId);
        if (Objects.nonNull(appInfo)) {
            appProcessorMap.put(appId, appInfo);
            System.err.println("替换成功，正式容器已包含此应用：" + appId);
            return true;
        }
        return false;
    }

    public static boolean removeTempMapApp(String appId) {
        // 从临时容器中查找到对象，并set到正式容器中
        AppInfo appInfo = appProcessorTempMap.get(appId);
        if(Objects.nonNull(appInfo)) {
            synchronized (lock) {
                appInfo = appProcessorTempMap.get(appId);
                if(Objects.nonNull(appInfo)) {
                    appProcessorTempMap.remove(appId);
                }
            }
        }
        return true;
    }

    /**
     * 应用的下线
     *
     * @param appId
     */
    public static void offlineApp(String appId) {
        AppInfo appInfo = getAppInfo(appId);
        appInfo.isOffline = true;
//        if (Objects.nonNull(appInfo)) {
//            Boolean isOffline = appInfo.getIsOffline();
//            if (!isOffline) {
//                synchronized (lock) {
//                    isOffline = appInfo.getIsOffline();
//                    if (!isOffline) {
//                        appInfo.isOffline = true;
//                    }
//                }
//            }
//        }
    }


    // 加载指定jar包下的processor
    public static void loadProcessor(String appId, String jarName) {

        String jarUrl1 = jarDir + File.separator + jarName;

        String jar2Name = "app-mock-1.0.0.1.jar";
        String jarUrl2 = jarDir + File.separator + jar2Name;

        long start = System.currentTimeMillis();
        // 扫描jar包下的类
        try {
            JarFile jarFile = new JarFile(new File(jarUrl1));
            List<String> processorSet = ClassScanner.getJarClasses(jarFile, "cn.com.infosec.mock.app.processor");

            JarClassLoader jarLoader = new JarClassLoader(new String[]{jarUrl1});
            Class<?> loadClass = jarLoader.loadClass(processorSet.get(0));
            Object o = loadClass.newInstance();
            System.err.println(o.toString());
            Method processor = loadClass.getMethod("processor");
            Object invoke = processor.invoke(o);
            System.err.println(invoke);
            Object processId = loadClass.getMethod("getProcessId").invoke(o);
            AppInfo appInfo = getAppInfo(appId);
            Map<String, Object> processorMap = null;
            if (Objects.isNull(appInfo)) {
                appInfo = new AppInfo();
                appInfo.setAppId(appId);
                processorMap = new ConcurrentHashMap<>();
            } else {
                processorMap = appInfo.getProcessors();
            }
            if (Objects.isNull(processorMap)) {
                processorMap = new ConcurrentHashMap<>();
            }
            appInfo.setProcessors(processorMap);
            processorMap.put(processId.toString(), o);
            appProcessorMap.put(appId, appInfo);


            JarFile jar2File = new JarFile(new File(jarUrl2));
            List<String> processorSet2 = ClassScanner.getJarClasses(jar2File, "cn.com.infosec.mock.app.processor");


            JarClassLoader jarLoader2 = new JarClassLoader(new String[]{jarUrl2});
            Class<?> loadClass2 = jarLoader2.loadClass(processorSet2.get(0));
            Object o2 = loadClass2.newInstance();
            System.err.println(o2.toString());
            Method processor2 = loadClass2.getMethod("processor");
            Object invoke2 = processor2.invoke(o2);
            System.err.println(invoke2);
            Object processId2 = loadClass2.getMethod("getProcessId").invoke(o2);
            Map<String, Object> processorTempMap = new ConcurrentHashMap<>();
            processorTempMap.put(processId2.toString(), o2);
            Map<String, Object> appProcessorTempMap = new ConcurrentHashMap<>();
            appProcessorTempMap.put(appId, processorTempMap);

//            JarClassLoaderSwapper classLoaderSwapper = JarClassLoaderSwapper.newCurrentThreadClassLoaderSwapper();
//            classLoaderSwapper.setCurrentThreadClassLoader(jarLoader);
//            Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass("cn.com.infosec.netsign.Application");
//            classLoaderSwapper.restoreCurrentThreadClassLoader();
//            Object o = aClass.newInstance();

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 加载指定目录下的processor --采用新版本应用放到临时容器的方式
     * 系统启动时加载
     *
     * @param appId      应用标识
     * @param classDir   class文件所在目录
     * @param packageDir processor的包名
     */
    public static void loadProcessor(String appId, String classDir, String packageDir, String version) {
        Map<String, AppInfo> map = appProcessorMap;
//        if (isExists) {
//            map = appProcessorTempMap;
//        }
        String appClassDir = classDir + File.separator + appId + File.separator + version + File.separator + "classes";
        if (appId.contains(".")) {
            appId = appId.substring(0, appId.lastIndexOf("."));
        }
        AppClassLoader classLoader = new AppClassLoader(new String[]{appClassDir});
        List<String> classes = ClassScanner.searchClasses(appClassDir, packageDir);
        for (String cls : classes) {
            try {
                Class<?> loadClass = classLoader.loadClass(cls);
                Object o = loadClass.newInstance();
                System.err.println(o);
//                Method processor = loadClass.getMethod("processor", String.class);
//                Object invoke = processor.invoke(o, appId);
//                System.err.println(invoke);
                Object processId = loadClass.getMethod("getProcessId").invoke(o);
                AppInfo appInfo = map.get(appId);
                Map<String, Object> processorMap = null;
                if (Objects.isNull(appInfo)) {
                    appInfo = new AppInfo();
                    appInfo.setAppId(appId);
                    processorMap = new ConcurrentHashMap<>();
                } else {
                    processorMap = appInfo.getProcessors();
                }
//                Map<String, Object> processorMap = appProcessorMap.get(appId);
                if (Objects.isNull(processorMap)) {
                    processorMap = new ConcurrentHashMap<>();
                }
                appInfo.setProcessors(processorMap);
                processorMap.put(processId.toString(), o);
                map.put(appId, appInfo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 加载指定目录下的processor --采用新版本应用放到临时容器的方式
     * 应用发布时加载
     *
     * @param appId      应用标识
     * @param classDir   class文件所在目录
     * @param packageDir processor的包名
     */
    public static AppInfo loadVersionProcessor(String appId, String classDir, String packageDir, String version) {
        String appClassDir = classDir + File.separator + appId + File.separator + version + File.separator + "classes";
        if (appId.contains(".")) {
            appId = appId.substring(0, appId.lastIndexOf("."));
        }
        AppClassLoader classLoader = new AppClassLoader(new String[]{appClassDir});
        List<String> classes = ClassScanner.searchClasses(appClassDir, packageDir);
        AppInfo appInfo = new AppInfo();
        appInfo.setAppId(appId);
        Map<String, Object> processorMap = new ConcurrentHashMap<>();
        appInfo.setProcessors(processorMap);
        for (String cls : classes) {
            try {
                Class<?> loadClass = classLoader.loadClass(cls);
                Object o = loadClass.newInstance();
                System.err.println(o);
//                Method processor = loadClass.getMethod("processor", String.class);
//                Object invoke = processor.invoke(o, appId);
//                System.err.println(invoke);
                Object processId = loadClass.getMethod("getProcessId").invoke(o);
                Map<String, Object> tempMap = appInfo.getProcessors();
                tempMap.put(processId.toString(), o);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return appInfo;
    }


    public static void main(String[] args) {
        String jarName = "app-mock-1.0.0.0.jar";
//        loadProcessor("demo",jarName);
        String dir = "D:\\test\\lib\\testapp\\classes";
        String packagePath = "cn.com.infosec.mock.app.processor";
        AppClassLoader classLoader = new AppClassLoader(new String[]{dir});
        List<String> classes = ClassScanner.searchClasses(dir, packagePath);
        for (String cls : classes) {
            try {
                Class<?> aClass = classLoader.loadClass(cls);
                Object o = aClass.newInstance();
                System.err.println(o);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
