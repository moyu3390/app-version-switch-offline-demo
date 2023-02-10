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
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
            if (Objects.isNull(appInfo) || appInfo.isOffline) {
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
        // map中不存在才能put成功,并发时，只会有一个线程put成功，返回值为null
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
    public static boolean replaceAppInfo(String appId) {
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
        if (Objects.nonNull(appInfo)) {
            Boolean isOffline = appInfo.getIsOffline();
            if (!isOffline) {
                synchronized (lock) {
                    isOffline = appInfo.getIsOffline();
                    if (!isOffline) {
                        appInfo.isOffline = true;
                    }
                }
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
    public static AppInfo loadProcessor(String appId, String classDir, String packageDir, String version) {
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
