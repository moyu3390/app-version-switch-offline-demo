/**
 * AppProcessorManager
 * <p> 应用事务管理-方式2，多版本替换下线采用临时容器存储旧应用的方式
 * 1.0
 * <p>
 * 2023/2/3 15:55
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

public class AppProcessorManager {
    private static volatile Map<String, AppInfo> appProcessorMap = new ConcurrentHashMap<>();
    private static volatile Map<String, AppInfo> appProcessorTempMap = new ConcurrentHashMap<>();
    public static final String jarDir = "D:\\test\\lib";

    public static final String packagePath = "cn.com.infosec.mock.app.processor";
    static Object lock = new Object();


    /**
     * 根据应用标识和事务标识查找对应的事务处理对象 --采用旧版本应用放到临时容器的方式
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

    private static AppInfo getAppInfo(String appId) {
        AppInfo appInfo = appProcessorMap.get(appId);
        return appInfo;
    }

    /**
     * 把旧应用从正式容器转移到临时容器 --采用旧版本应用放到临时容器的方式
     *
     * @param appId
     * @return
     */
    public static boolean copyAppInfo2TempMap(String appId) {
        // 从正式容器中查找到对象，并set到临时容器中
        AppInfo appInfo = appProcessorMap.get(appId);
        if (Objects.nonNull(appInfo)) {
            appProcessorTempMap.put(appId, appInfo);
            System.err.println("复制成功，临时容器已包含此应用：" + appId);
            return true;
        }
        return false;
    }

    public static boolean replaceAppInfo(AppInfo appInfo) {
        appProcessorMap.put(appInfo.getAppId(), appInfo);
        return true;
    }

    /**
     * 应用的下线--临时容器中应用的下线
     *
     * @param appId
     */
    public static void offlineApp(String appId) {
        AppInfo appInfo = appProcessorTempMap.get(appId);
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
     * 从正式容器中移除指定的应用信息 --采用旧版本应用放到临时容器的方式
     *
     * @param appId
     * @return
     */
    public static boolean removeAppFromTempMap(String appId) {
        AppInfo remove = appProcessorTempMap.remove(appId);
        return true;
    }

    /**
     * 获取指定的下线的应用信息
     *
     * @param appId
     * @return
     */
    public static AppInfo getOfflineApp(String appId) {
        return appProcessorTempMap.get(appId);
    }

    /**
     * 从正式容器中移除指定的应用信息 --采用旧版本应用放到临时容器的方式
     *
     * @param appInfo
     * @return
     */
    public static boolean setAppInfoIfAbsent(AppInfo appInfo) {
        // map中不存在才能put成功,并发时，只会有一个线程put成功，返回值为null
        appProcessorMap.putIfAbsent(appInfo.getAppId(), appInfo);
        return true;
    }


    /**
     * 加载指定目录下的processor --采用新版本应用放到临时容器的方式
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


}
