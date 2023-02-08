/**
 * ClassScanner
 * <p>
 * 1.0
 * <p>
 * 2023/2/1 17:18
 */

package com.hclteam.moyu3390.app.offline.demo.loaders;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ClassScanner {
    static FileFilter classFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".class");
        }
    };

    //从包路径下扫描
    public static Set<Class> getClasses(String packagePath) {
        Set<Class> res = new HashSet<>();
        String path = packagePath.replace(".", "/");
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            System.out.println(packagePath + " is not exit");
            return res;
        }
        String protocol = url.getProtocol();
        if ("jar".equalsIgnoreCase(protocol)) {
            try {
                res.addAll(getJarClasses(url, packagePath));
            } catch (IOException e) {
                e.printStackTrace();
                return res;
            }
        } else if ("file".equalsIgnoreCase(protocol)) {
            res.addAll(getFileClasses(url, packagePath));
        }
        return res;
    }

    //获取file路径下的class文件
    private static Set<Class> getFileClasses(URL url, String packagePath) {
        Set<Class> res = new HashSet<>();
        String filePath = url.getFile();
        File dir = new File(filePath);
        String[] list = dir.list();
        if (list == null) return res;
        for (String classPath : list) {
            if (classPath.endsWith(".class")) {
                classPath = classPath.replace(".class", "");
                try {
                    Class<?> aClass = Class.forName(packagePath + "." + classPath);
                    res.add(aClass);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                res.addAll(getClasses(packagePath + "." + classPath));
            }
        }
        return res;
    }

    //使用JarURLConnection类获取路径下的所有类
    public static Set<Class> getJarClasses(URL url, String packagePath) throws IOException {
        Set<Class> res = new HashSet<>();
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        if (conn != null) {
            JarFile jarFile = conn.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.contains(".class") && name.replaceAll("/", ".").startsWith(packagePath)) {
                    String className = name.substring(0, name.lastIndexOf(".")).replace("/", ".");
                    try {
                        Class clazz = Class.forName(className);
                        res.add(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return res;
    }


    public static void getClasses(String classDir, List<File> classList) {
        File dir = new File(classDir);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (File a : files) {
            if (a.isDirectory()) {
                getClasses(a.getAbsolutePath(), classList);
            }
            if (a.getName().endsWith(".class")) {
                classList.add(a);
            }
        }
    }


    public static List<String> getJarClasses(JarFile jarFile, String packagePath) throws IOException {
        List<String> res = new ArrayList<>();
        if (jarFile != null) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.contains(".class") && name.replaceAll("/", ".").startsWith(packagePath)) {
                    try {
                        String className = name.substring(0, name.lastIndexOf(".")).replace("/", ".");
//                        Class clazz = Class.forName(className);
                        res.add(className);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return res;
    }

    public static void main(String[] args) {
//        String jarDir = "D:\\test\\lib\\app-mock-1.0.0.0.jar";
//        File jarFile = new File(jarDir);
//        try {
//            JarFile file = new JarFile(jarFile);
//            List<String> jarClasses = getJarClasses(file, "cn.com.infosec.mock.app.processor");
//            System.err.println(jarClasses);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        String packagePath = "cn.com.infosec.mock.app.processor";
        String classDir = "D:\\test\\lib\\testapp\\classes";
        List<String> clazz = searchClasses( classDir,packagePath);
        for (String f : clazz) {

            System.err.println(f);
        }


    }

    public static List<String> searchClasses(String classDir, String packagePath) {
        String packageDir = packagePath.replaceAll("\\.", "/");
        String classPath = classDir + File.separator + packageDir;

        List<File> clazzList = new ArrayList<>();
        getClasses(classPath, clazzList);
        List<String> clazz = clazzList.stream().map(c -> {
            String replace = c.getAbsolutePath().replace(classDir, "").substring(1);
            replace = replace.replace("\\", ".");
            replace= replace.replaceAll(".class","");
            return replace;
        }).collect(Collectors.toList());
        return clazz;
    }
}
