/**
 * AppService2
 * <p> 方式2，多版本替换下线采用临时容器存储旧应用的方式
 * 1.0
 * <p>
 * 2023/2/3 16:02
 */

package com.hclteam.moyu3390.app.offline.demo.web.service;

import com.hclteam.moyu3390.app.offline.demo.processor.AppProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.processor.ProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import com.hclteam.moyu3390.app.offline.demo.web.runnable.AppOfflineRunnableV2;
import com.hclteam.moyu3390.app.offline.demo.web.runnable.AppThreadCaller;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
public class AppService2 {
    private static Map<String, Future<Boolean>> appResultMap = new ConcurrentHashMap<>();
    // 应用延迟下线时间(毫秒)
    private static int delayTime = 1 * 20 * 1000;

    /**
     * 模拟发布应用的主要逻辑
     *
     * @param appId
     * @return
     */
    public Object publishApp(String appId, String version) {
        // 检查是否有同名应用在提供服务
        Boolean isRunning = AppProcessorManager.isRunning(appId);
        System.err.println(AppService2.class.getSimpleName() + "=====新应用的发布任务，" + appId + "---" + version);
        // 如果应用已存在，说明有旧版本正在运行

        if (isRunning) {
            // todo 如果后期并发量高，此处可以做成doubble check 方式来获取应用的返回结果信息
            Future<Boolean> future = appResultMap.get(appId);
            // 如果当前应用的处理线程不存在或已经处理结束(不关心处理成功还是失败)，则可以重新发布应用，否则不发布
            if (Objects.isNull(future) || future.isDone()) {
                //  旧应用存活时间开始倒计时（2分钟），激活下线线程
                //  开启线程计时

                AppOfflineRunnableV2 task = new AppOfflineRunnableV2(delayTime, appId, version);
                // 清理线程等待倒计时结束，开始卸载旧应用的classloader和已加载的实例信息
                Future<Boolean> booleanFuture = AppThreadCaller.call(task, Boolean.TRUE);
                appResultMap.put(appId, booleanFuture);
            } else {
                System.err.println(AppService2.class.getSimpleName() + "=====当前系统已存在该应用的发布任务，请稍后继续发布..." + appId);
            }
            return Boolean.TRUE;
        }
        //  加载新版本应用的事务对象，返回appinfo对象
        AppInfo appInfo = AppProcessorManager.loadProcessor(appId, ProcessorManager.jarDir, ProcessorManager.packagePath, version);
        AppProcessorManager.setAppInfoIfAbsent(appInfo);
        return Boolean.TRUE;
    }
}
