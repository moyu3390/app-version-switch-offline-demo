/**
 * JarClassLoader
 * <p>
 * 1.0
 * <p>
 * 2023/1/19 17:27
 */

package com.hclteam.moyu3390.app.offline.demo.loaders;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassLoader extends URLClassLoader {
    private static ThreadLocal<URL[]> threadLocal = new ThreadLocal<>();
    private URL[] allUrl;


    public JarClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        allUrl = threadLocal.get();
    }

    public JarClassLoader(String[] urls) {
        this(urls, null);
    }

    public JarClassLoader(String[] urls, ClassLoader parent) {
        this(getURLs(urls), parent);
    }

    /**
     * description 通过文件目录获取目录下所有的jar全路径信息
     *
     * @param paths 文件路径
     * @return java.net.URL[]
     * @author Cyber
     * <p> Created by 2022/11/22
     */
    private static URL[] getURLs(String[] paths) {
        if (null == paths || 0 == paths.length) {
            throw new RuntimeException("jar包路径不能为空.");
        }
        List<String> dirs = new ArrayList<String>();
        for (String path : paths) {
            dirs.add(path);
            JarClassLoader.collectDirs(path, dirs);
        }
        List<URL> urls = new ArrayList<URL>();
        for (String path : dirs) {
            urls.addAll(doGetURLs(path));
        }
        URL[] threadLocalurls = urls.toArray(new URL[0]);
        threadLocal.set(threadLocalurls);
        return threadLocalurls;
    }

    /**
     * description 递归获取文件目录下的根目录
     *
     * @param path      文件路径
     * @param collector 根目录
     * @return void
     * @author Cyber
     * <p> Created by 2022/11/22
     */
    private static void collectDirs(String path, List<String> collector) {
        if (null == path || "".equalsIgnoreCase(path)) {
            return;
        }
        File current = new File(path);
        if (!current.exists() || !current.isDirectory()) {
            return;
        }
        for (File child : current.listFiles()) {
            if (!child.isDirectory()) {
                continue;
            }
            collector.add(child.getAbsolutePath());
            collectDirs(child.getAbsolutePath(), collector);
        }
    }

    private static List<URL> doGetURLs(final String path) {
        if (null == path || "".equalsIgnoreCase(path)) {
            throw new RuntimeException("jar包路径不能为空.");
        }
        File jarPath = new File(path);
//        if (!jarPath.exists() || !jarPath.isDirectory()) {
//            throw new RuntimeException("jar包路径必须存在且为目录.");
//        }

//        FileFilter jarFilter = new FileFilter() {
//            /**
//             * description  判断是否是jar文件
//             * @param pathname jar 全路径文件
//             * @return boolean
//             * @author Cyber
//             * <p> Created by 2022/11/22
//             */
//            @Override
//            public boolean accept(File pathname) {
//                return pathname.getName().endsWith(".jar");
//            }
//        };
//        File[] allJars = new File(path).listFiles(jarFilter);
        File[] allJars = {new File(path)};
        List<URL> jarURLs = new ArrayList<URL>(allJars.length);
        for (int i = 0; i < allJars.length; i++) {
            try {
                jarURLs.add(allJars[i].toURI().toURL());
            } catch (Exception e) {
                throw new RuntimeException("系统加载jar包出错", e);
            }
        }
        return jarURLs;
    }

    /**
     * description 重新loadClass加载过程，打破双亲委派，采用逆向双亲委派
     *
     * @param className 加载的类名
     * @return java.lang.Class<?>
     */
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (allUrl != null) {
            String classPath = className.replace(".", "/");
            classPath = classPath.concat(".class");
            for (URL url : allUrl) {
                byte[] data = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = null;
                try {
                    File file = new File(url.toURI());
                    if (file != null && file.exists()) {
                        JarFile jarFile = new JarFile(file);
                        if (jarFile != null) {
                            JarEntry jarEntry = jarFile.getJarEntry(classPath);
                            if (jarEntry != null) {
                                is = jarFile.getInputStream(jarEntry);
                                int c = 0;
                                while (-1 != (c = is.read())) {
                                    baos.write(c);
                                }
                                data = baos.toByteArray();
                                System.out.println("********找到classPath=" + classPath + "的jar=" + url.toURI().getPath() + "*******");
                                return this.defineClass(className, data, 0, data.length);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        // 未找到的情况下通过父类加载器加载
        return super.loadClass(className);
    }
}
