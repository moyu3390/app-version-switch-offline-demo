/**
 * AppClassLoader
 * <p>
 * 1.0
 * <p>
 * 2023/2/2 14:49
 */

package com.hclteam.moyu3390.app.offline.demo.loaders;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AppClassLoader extends URLClassLoader {
    private URL[] allUrl;

    public AppClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        allUrl = urls;
    }

    /**
     * 应用 class的目录地址
     *
     * @param urls
     */
    public AppClassLoader(String[] urls) {
        this(urls, null);
    }

    public AppClassLoader(String[] urls, ClassLoader parent) {
        this(getUrls(urls), parent);
    }

    private static URL[] getUrls(String[] uls) {
        // 转换成url数组
        List<URL> urlList = new ArrayList<>();
        for (String url : uls) {
            url = "file:/" + url;
//            File file = new File(url);
            try {
//                new URL(url);
                urlList.add(new URL(url));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        URL[] us = new URL[urlList.size()];
        urlList.toArray(us);
        return us;
    }

    @Override
    protected Class<?> findClass(String name) {
        Class<?> clz = null;
        String path = name.replace('.', '/').concat(".class");
        for (URL url : allUrl) {
            String classDir = url.getFile();
            classDir = classDir + File.separator + path;
            File classFile = new File(classDir);
            if (!classFile.exists()) {
                continue;
            }
            try {
                byte[] classBytes = getClassBytes(classFile);
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (Throwable e) {
                throw new RuntimeException("load class error：" + name);
            }
        }
        return clz;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> aClass = findClass(name);
        if (Objects.nonNull(aClass)) {
            return aClass;
        }
        return super.loadClass(name);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.err.println(this.getClass().getName() + "被回收");
        Class<?>[] classes = this.getClass().getClasses();
        Arrays.stream(classes).forEach(c -> {
            System.err.println(this.getClass().getName() + "加载的类：" + c.getName());
        });
    }

    private byte[] getClassBytes(File classfile) {
        try (InputStream fis = new FileInputStream(classfile); ByteArrayOutputStream classBytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                classBytes.write(buffer, 0, len);
            }
            return classBytes.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
