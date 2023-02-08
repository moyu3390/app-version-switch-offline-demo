/**
 * AppOfflineRunnableV2
 * <p>
 * 1.0
 * <p>
 * 2023/2/3 17:36
 */

package com.hclteam.moyu3390.app.offline.demo.web.runnable;

import com.hclteam.moyu3390.app.offline.demo.processor.AppProcessorManager;
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

    public AppOfflineRunnableV2(long delayTime, String appId) {
        this.delayTimeMillis = delayTime;
        this.appId = appId;
    }

    @Override
    public void run() {
        // 先延迟等待
        sleep();
        // 线程没有中断才执行逻辑代码
        if (!Thread.currentThread().isInterrupted()) {
            // 开始卸载旧应用的classloader和已加载的实例信息
            AppInfo appInfo = AppProcessorManager.getOfflineApp(appId);
            if (Objects.isNull(appInfo)) {
                System.err.println(Thread.currentThread().getName() + " 临时容器中已不存在应用：" + appId);
                return;
            }
            // 检查是否为下线状态
            Boolean isOffline = appInfo.isOffline;
            System.err.println(Thread.currentThread().getName() + " 临时容器中应用在线状态 isOffline：" + isOffline);
            if (isOffline) {
                System.err.println(Thread.currentThread().getName() + " 删除临时容器中的应用：" + appId);
                sleep(5 * 1000);
                AppProcessorManager.removeAppFromTempMap(appId);
                // 获取classloader
                URLClassLoader classLoader = (URLClassLoader) appInfo.getClass().getClassLoader();

                while (true) {
                    try {
                        classLoader.close();
                    } catch (IOException e) {
                    }
                    classLoader = null;
                    // 卸载classloader
                    WeakReference<ClassLoader> classLoaderWeakReference = new WeakReference<>(classLoader);

                    // 置位弱引用,等下一次垃圾回收
                    appInfo = null;
                    WeakReference<AppInfo> appInfoWeakReference = new WeakReference<>(appInfo);

                    if (appInfoWeakReference.get() == null && classLoaderWeakReference.get() == null) {
                        System.err.println(Thread.currentThread().getName() + " appinfo和classloader 被回收");
                        break;
                    }
                    try {
                        System.err.println(Thread.currentThread().getName() + " 检测是否被回收");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
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
