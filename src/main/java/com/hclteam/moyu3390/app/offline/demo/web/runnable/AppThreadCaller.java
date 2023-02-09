/**
 * AppThreadCaller
 * <p>
 * 1.0
 * <p>
 * 2023/2/3 9:49
 */

package com.hclteam.moyu3390.app.offline.demo.web.runnable;

import java.util.concurrent.*;

public class AppThreadCaller {

    //数据处理时需要强关联数据时间顺序时，换一种拒绝策略。CallerRunsPolicy不适合时间顺序要求比较严格的场景
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200), Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());


    public static <T> Future<T> call(Runnable runnable,T result) {
        Future<T> submit = executor.submit(runnable, result);
        printThreadPoolStatus(executor);
        return submit;
    }


    private static void printThreadPoolStatus(ThreadPoolExecutor executor) {
        BlockingQueue queue = executor.getQueue();
        System.out.println(Thread.currentThread().getName() + "," +

                "当前的线程数量:" + executor.getPoolSize() + "," +
                "核心线程数:" + executor.getCorePoolSize() + "," +
                "最大线程数:" + executor.getMaximumPoolSize() + "," +
                "同时存在的最大线程数:" + executor.getLargestPoolSize() + "," +
                "活动线程数:" + executor.getActiveCount() + "," +
                "任务总数:" + executor.getTaskCount() + "," +
                "任务完成数:" + executor.getCompletedTaskCount() + "," +
                "线程空闲时间:" + executor.getKeepAliveTime(TimeUnit.SECONDS) + "秒," +
                "当前排队线程数:" + queue.size() + "," +
                "队列剩余大小:" + queue.remainingCapacity() + "," +
                "线程池是否关闭:" + executor.isShutdown() + ","
        );
    }
}
