package com.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import ljystu.project.callgraph.utils.JarReadUtils;
import ljystu.project.callgraph.utils.PackageUtils;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarTest {
    @Test
    public void linkTest() {
        String jar = "hamcrest-core-1.3.jar";
//        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        String rootPath = "junit4-main";
        HashSet<String> dependencies = new HashSet<>();
//        PackageUtils.getJarToCoordMap(rootPath,jarName,  coord);
        System.out.println(PackageUtils.jarToCoordMap.toString());
//        assertThat(PackageUtils.jarToCoordMap.contains(jar),true);
        if (PackageUtils.jarToCoordMap.containsKey(jar)) {
            System.out.println(PackageUtils.jarToCoordMap.get(jar));
        }

    }

    @Test
    public void redisTest() {
        Jedis jedis = new Jedis("localhost");

        List<String> list = new ArrayList<>();
        list.add("com.package1");
        list.add("com.pkg2");
        String listJson = JSON.toJSONString(list);
        jedis.hset("myjar", "package_list", listJson);
        jedis.hset("myjar", "coordinate_name", "group:artifact:1.0.0");

        String hget = jedis.hget("myjar", "package_list");
        List<String> strings = JSONObject.parseArray(hget, String.class);
        System.out.println(strings.toString());

    }

    @Test
    public void readJarTest() throws IOException {
        String jarFilePath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";
        String tempDir = "src/main/resources/" + "javacg-0.1-SNAPSHOT-dycg-agent";

        try (ZipFile zipFile = new ZipFile(jarFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                File file = new File(tempDir, entry.getName());
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (InputStream input = zipFile.getInputStream(entry);
                     OutputStream output = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        Set<String> classes = JarReadUtils.getClasses(tempDir);
        System.out.println(classes.size());
        Set<String> importedPackages = new HashSet<>();
        try {
            importedPackages = importInfo(jarFilePath, classes, stringBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String importedPackage : importedPackages) {
            System.out.println(importedPackage);
        }
//        assertThat(importedPackages.size(), greaterThan(0));  // Fest assertion
    }

    public static Set<String> importInfo(String jarFile, Set<String> classes, StringBuilder packages) throws Exception {
        Set<String> importedPackages = new HashSet<>();
        Set<String> selfPackages = new HashSet<>();

        for (String clazz : classes) {
//            Class cls = URLClassLoader.newInstance(new URL[]{url}).loadClass(clazz);

            String classFilePath = clazz;
            try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(classFilePath)))) {
                ClassFile classFile = new ClassFile(dis);

                String name = classFile.getName();

                ClassPool pool = ClassPool.getDefault();

                pool.appendClassPath(jarFile);
                CtClass cc = pool.get(name);
                Collection<String> refClasses = cc.getRefClasses();
//                System.out.println("className : " + name);
                for (String refClass : refClasses) {
//                    System.out.println(refClass);
                    CtClass importedClass;
                    try {
                        importedClass = pool.get(refClass);
                    } catch (NotFoundException e) {
//                        System.out.println( refClass+" not in jar!");
                        continue;
                    }

                    String importedClassPackageName = importedClass.getPackageName();
                    importedPackages.add(importedClassPackageName);
                }


                String packageName = cc.getPackageName();
                if (packageName.lastIndexOf(".") == -1) {
                    selfPackages.add(packageName);
                } else {
                    String substring = packageName.substring(0, packageName.lastIndexOf(".")) + ".*|";
                    selfPackages.add(substring);
                }
            }
        }

        for (String pkg : selfPackages) {
            packages.append(pkg);
        }
        if (packages.length() > 0) {
            packages.setLength(packages.length() - 1);
        }
        System.out.println(importedPackages.size());
        return importedPackages;
    }

    @Test
    public void readJarWithoutUnzip() throws IOException {
        // code blocks syntax from markdown
// assume jarFiles is an array of JarFile objects
// assume packageNames is a set of String objects
        Set<String> packageNames = new HashSet<>();

        File file = new File("/Users/ljystu/Downloads/jython-2.7.3.jar");
        long length = file.length();

        long tenMegabytes = 10485760L; // 10MB的字节数

        if (length > tenMegabytes) {
            System.out.println("Byte value is greater than 10MB");
        } else {
            System.out.println("Byte value is less than or equal to 10MB");
        }

    }

}
