/**
 * AppOfflineRunnable
 * <p>
 * 1.0
 * <p>
 * 2023/2/3 9:47
 */

package com.hclteam.moyu3390.app.offline.demo.web.runnable;

import com.hclteam.moyu3390.app.offline.demo.processor.ProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Vector;

@Slf4j
public class AppOfflineRunnable implements Runnable {
    // 延迟执行时间:单位（毫秒）
    private long delayTimeMillis;
    private String appId;
    private String version;


    public AppOfflineRunnable(long delayTime, String appId, String _version) {
        this.delayTimeMillis = delayTime;
        this.appId = appId;
        this.version = _version;
    }

    @Override
    public synchronized void run() {
        AppInfo appInfo = ProcessorManager.loadProcessor(appId, ProcessorManager.jarDir, ProcessorManager.packagePath, version);
        // 把应用放入临时容器
        ProcessorManager.setApp2TempMap(appInfo);
        /**  等待新应用加载完毕后，为旧应用停止提供processor服务，切换为新应用的processor实例
         修改旧应用的下线状态为true，在获取processor的方法中改造。获取临时容器中应用的processor */
        // 旧应用下线
        ProcessorManager.offlineApp(appId);
        // 先延迟等待
        sleep();
        // 线程没有中断才执行逻辑代码
        System.err.println(AppOfflineRunnable.class.getSimpleName() + "=====" + Thread.currentThread().getName() + " 当前线程中断状态:" + Thread.currentThread().isInterrupted());
        // 下面逻辑主要是替换应用信息和删除临时容器中的应用，其他功能只是辅助
        if (!Thread.currentThread().isInterrupted()) {
            // 开始卸载旧应用的classloader和已加载的实例信息
            AppInfo appInfo1 = ProcessorManager.getOfflineApp(appId);
            // 检查是否为下线状态
            Boolean isOffline = appInfo1.isOffline;
            System.err.println(AppOfflineRunnable.class.getSimpleName() + "=====" + Thread.currentThread().getName() + " 正式容器中旧应用在线状态 isOffline：" + isOffline);
            if (isOffline) {
                // 替换正式容器中的appInfo
                boolean isReplaced = ProcessorManager.replaceAppInfo(appId);
                System.err.println(AppOfflineRunnable.class.getSimpleName() + "=====" + Thread.currentThread().getName() + " 应用是否替换成功？" + isReplaced);
                // 获取classloader
                URLClassLoader classLoader = (URLClassLoader) appInfo1.getClass().getClassLoader();
                WeakReference<AppInfo> appInfoWeakReference = new WeakReference<>(appInfo1);
                WeakReference<URLClassLoader> classLoaderWeakReference = new WeakReference<>(classLoader);
                if (isReplaced) {
                    sleep(5 * 1000);
                    // 清除临时容器中的新应用
                    ProcessorManager.removeTempMapApp(appId);
                }
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
//            classes.clear();
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
