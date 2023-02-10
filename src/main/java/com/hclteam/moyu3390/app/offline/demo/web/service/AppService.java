/**
 * AppService
 * <p> 方式1，多版本替换下线采用临时容器存储新应用的方式
 * 1.0
 * <p>
 * 2023/2/2 9:59
 */

package com.hclteam.moyu3390.app.offline.demo.web.service;

import com.hclteam.moyu3390.app.offline.demo.processor.ProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import com.hclteam.moyu3390.app.offline.demo.web.runnable.AppOfflineRunnable;
import com.hclteam.moyu3390.app.offline.demo.web.runnable.AppThreadCaller;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
public class AppService {

    public List<String> getAppOrders(String appId) {
        Object processor = ProcessorManager.getProcessor(appId, "GetAppOrders");
        if (Objects.isNull(processor)) {
            return Collections.emptyList();
        }
        try {
            Method getAppOrders = processor.getClass().getMethod("processor", String.class);
            Method getVersion = processor.getClass().getMethod("getVersion");
            Object version = getVersion.invoke(processor);
            System.err.println("应用版本：" + version);
            Object orders = getAppOrders.invoke(processor, appId);
            System.err.println("==========================================================================");
            if (orders instanceof List) {
                return (List<String>) orders;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static Map<String, Future<Boolean>> appResultMap = new ConcurrentHashMap<>();


    /**
     * 模拟发布应用的主要逻辑
     *
     * @param appId
     * @return
     */
    public Object publishApp(String appId, String version) {
        // 检查是否有同名应用在提供服务
        Boolean isRunning = ProcessorManager.isRunning(appId);
        System.err.println(AppService.class.getSimpleName() + "=====新应用的发布任务，" + appId + "---" + version);

        // 如果应用已存在，说明有旧版本正在运行
        // 加载新应用的porcessor,如果不存在，加载到正式容器中，如果存在，加载到临时容器中。此时，旧应用还在提供服务
        if (isRunning) {
            // 如果后期并发量高，此处可以做成doubble check 方式来获取应用的返回结果信息
            Future<Boolean> future = appResultMap.get(appId);
            // 如果当前应用的处理线程不存在或已经处理结束(不关心处理成功还是失败)，则可以重新发布应用，否则不发布
            if (Objects.isNull(future) || future.isDone()) {
                //  旧应用存活时间开始倒计时（2分钟），激活下线线程
                //  开启线程计时
                AppOfflineRunnable task = new AppOfflineRunnable(1 * 10 * 1000, appId, version);
                // 清理线程等待倒计时结束，开始卸载旧应用的classloader和已加载的实例信息
                // 把新应用暂存的信息 保存到应用信息容器中  完成旧应用的替换
                Future<Boolean> booleanFuture = AppThreadCaller.call(task, Boolean.TRUE);
                appResultMap.put(appId, booleanFuture);
            } else {
                System.err.println(AppService.class.getSimpleName() + "=====当前系统已存在该应用的发布任务，请稍后继续发布..." + appId);
            }
            return Boolean.TRUE;
        }
        AppInfo appInfo = ProcessorManager.loadProcessor(appId, ProcessorManager.jarDir, ProcessorManager.packagePath, version);
        boolean b = ProcessorManager.setAppInfoIfAbsent(appInfo);

        return Boolean.TRUE;
    }


}
