/**
 * AppOfflineRunnableV2
 * <p>
 * 1.0
 * <p>
 * 2023/2/3 17:36
 */

package com.hclteam.moyu3390.app.offline.demo.web.runnable;

import com.hclteam.moyu3390.app.offline.demo.processor.AppProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.processor.ProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

@Slf4j
public class AppOfflineRunnableV2 implements Runnable {
    // 延迟执行时间:单位（毫秒）
    private long delayTimeMillis;
    private String appId;

    private String version;

    public AppOfflineRunnableV2(long delayTime, String appId, String _version) {
        this.delayTimeMillis = delayTime;
        this.appId = appId;
        this.version = _version;
    }

    @Override
    public void run() {
        AppInfo appInfo = AppProcessorManager.loadProcessor(appId, ProcessorManager.jarDir, ProcessorManager.packagePath, version);
        // 把旧版本应用复制至临时容器 ，此时两个容器中同时存在旧版本应用
        boolean isCopied = AppProcessorManager.copyAppInfo2TempMap(appId);
        // 替换正式容器中的应用对象，在此之前，旧对象在提供服务
        // 把新版本应用替换到正式容器中，此时正式容器中存在新版本应用，临时容器中存在旧版本应用
        AppProcessorManager.replaceAppInfo(appInfo);
        //  更改临时容器中旧版本应用的下线状态为true，更改为下线后，旧版本应用不再提供服务
        AppProcessorManager.offlineApp(appId);
        // 先延迟等待
        sleep();
        // 线程没有中断才执行逻辑代码
        // 下面逻辑主要是删除临时容器中的应用，其他功能只是辅助
        if (!Thread.currentThread().isInterrupted()) {
            // 开始卸载旧应用的classloader和已加载的实例信息
            AppInfo appInfo1 = AppProcessorManager.getOfflineApp(appId);
            if (Objects.isNull(appInfo1)) {
                System.err.println(AppOfflineRunnableV2.class.getSimpleName() + "=====" + Thread.currentThread().getName() + " 临时容器中已不存在应用：" + appId);
                return;
            }
            // 检查是否为下线状态
            Boolean isOffline = appInfo1.isOffline;
            System.err.println(AppOfflineRunnableV2.class.getSimpleName() + "=====" + Thread.currentThread().getName() + " 临时容器中应用在线状态 isOffline：" + isOffline);
            if (isOffline) {
                System.err.println(AppOfflineRunnableV2.class.getSimpleName() + "=====" + Thread.currentThread().getName() + " 删除临时容器中的应用：" + appId);
                sleep(5 * 1000);
                // 删除临时容器中的应用
                AppProcessorManager.removeAppFromTempMap(appId);
                // 获取classloader
                URLClassLoader classLoader = (URLClassLoader) appInfo1.getClass().getClassLoader();
                WeakReference<AppInfo> appInfoWeakReference = new WeakReference<>(appInfo1);
                WeakReference<URLClassLoader> classLoaderWeakReference = new WeakReference<>(classLoader);
                try {
                    classLoader.close();
                } catch (IOException e) {
                } finally {
                    appInfo1 = null;
                    classLoader = null;
                    appInfoWeakReference.clear();
                    classLoaderWeakReference.clear();
                    // 卸载classloader
                    appInfoWeakReference = null;
                    classLoaderWeakReference = null;
                }
            }
        }
    }


    public List<Class<?>> getAllLoadedClassesFromClassLoader(ClassLoader classLoader) {
        try {
            Class<?> loaderClass = classLoader.getClass();
            while (loaderClass != ClassLoader.class) {
                loaderClass = loaderClass.getSuperclass();
            }
            java.lang.reflect.Field classesField = loaderClass.getDeclaredField("classes");
            classesField.setAccessible(true);
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);
            List list = classes.subList(0, classes.size() - 1);
            return list;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void sleep() {
        sleep(delayTimeMillis);
    }

    private void sleep(long timeMillis) {
        try {
            if (timeMillis > 0) {
                log.info("延迟：【{}】 毫秒", timeMillis);
                Thread.sleep(timeMillis);
            }
        } catch (InterruptedException e) {
            // 此处异常捕获到的同时，线程的中断状态会自动清除。此处仅打印说明，不做其他处理
            log.warn("捕获到线程终端异常，{}", e);
        }
    }
}
