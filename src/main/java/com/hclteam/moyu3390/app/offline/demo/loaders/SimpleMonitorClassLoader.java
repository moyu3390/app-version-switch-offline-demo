/**
 * SimpleMonitorClassLoader
 * <p>
 * 1.0
 * <p>
 * 2023/2/9 18:02
 */

package com.hclteam.moyu3390.app.offline.demo.loaders;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单classloader监控类
 */
public class SimpleMonitorClassLoader {
    public static void main(String[] args) throws Exception {
        simpleGCDemo();

        // Guava的FinalizableReference机制
    }





    /**
     * 验证classloader、class和对象回收的时机。前两者是一起回收（因为ClassLoader及其加载的Class之间是相互引用的关系，要么同时GC Root可达，要么同时不可达）
     * @throws Exception
     */
    private static void simpleGCDemo() throws Exception {
        final ReferenceQueue<Object> rq = new ReferenceQueue<Object>();
        final Map<Object, Object> map = new HashMap<>();
        Thread thread = new Thread(() -> {
            try {
                WeakReference<byte[]> k;
                while((k = (WeakReference) rq.remove()) != null) {
                    System.out.println("GC回收了:" + map.get(k));
                }
            } catch(InterruptedException e) {
                //结束循环
            }
        });
        thread.setDaemon(true);
        thread.start();

        ClassLoader cl = newLoader();
        Class cls = cl.loadClass("com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo");
        Object obj = cls.newInstance();


        Object value = new Object();

        WeakReference<ClassLoader> weakReference = new WeakReference<ClassLoader>(cl, rq);
        map.put(weakReference, "ClassLoader URLClassLoader");
        WeakReference<Class> weakReference1 = new WeakReference<Class>(cls, rq);
        map.put(weakReference1, "Class classloader.test.Foo");
        WeakReference<Object> weakReference2 = new WeakReference<Object>(obj, rq);
        map.put(weakReference2, "Instance of Foo");

        obj=null;
        System.out.println("Set instance null and execute gc!");
        System.gc();
        Thread.sleep(3000);
        cls=null;
        System.out.println("Set class null and execute gc!");
        System.gc();
        Thread.sleep(3000);
        cl=null;
        System.out.println("Set classloader null and execute gc!");
        System.gc();
        Thread.sleep(3000);
    }

    static URLClassLoader newLoader() throws Exception{
        URL url = new File("E:\\workspaces\\idea-workspaces\\moyu3390\\app-version-switch-offline-demo\\target\\classes").toURI().toURL();
        URLClassLoader ucl = new URLClassLoader(new URL[] {url}, null);
        return ucl;
    }

}
