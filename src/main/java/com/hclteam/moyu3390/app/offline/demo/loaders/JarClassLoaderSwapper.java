/**
 * JarClassLoaderSwapper
 * <p>
 * 1.0
 * <p>
 * 2023/1/19 17:49
 */

package com.hclteam.moyu3390.app.offline.demo.loaders;

/**
 * description 自定义classloader加载器当前线程的ContextClassLoader处理
 */
public class JarClassLoaderSwapper {
    private ClassLoader storeClassLoader = null;

    private JarClassLoaderSwapper() {
    }

    public static JarClassLoaderSwapper newCurrentThreadClassLoaderSwapper() {
        return new JarClassLoaderSwapper();
    }

    /**
     * description 保存当前classLoader，并将当前线程的classLoader设置为所给classLoader
     *
     * @param classLoader
     * @return java.lang.ClassLoader
     */
    public ClassLoader setCurrentThreadClassLoader(ClassLoader classLoader) {
        this.storeClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        return this.storeClassLoader;
    }

    /**
     * description 将当前线程的类加载器设置为保存的类加载
     *
     * @param
     * @return java.lang.ClassLoader
     */
    public ClassLoader restoreCurrentThreadClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.storeClassLoader);
        return classLoader;
    }
}
