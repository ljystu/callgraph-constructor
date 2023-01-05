package com.example.neo4jtest.util;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.internal.batchimport.cache.idmapping.string.Radix;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class PackageUtil {
    static String argLineLeft = "-noverify -javaagent:";
    static String getArgLineRight = "=incl=";

    static List<String> paths = new ArrayList<>();

//    public static HashSet<Package> getCLasses(String rootPath) throws Exception {
//        HashSet<Package> definedPackages = new HashSet<>();
//        invokerUtil.invoke(new StringBuilder(), rootPath, "compile");
//        findTargetDirs(new File(rootPath));
//        for (String path : paths) {
////            ClassUtil finder = new ClassUtil(new File(path + "/classes/"));
//
//            List<String> classFiles = JavaReadUtil.getClasses(path.substring(0,path.lastIndexOf('/')) );
////                    finder.getClassFiles();
////            File dir = new File(path + "/classes/" );
////            URL url = dir.toURI().toURL();
////            URL[] urls = new URL[]{url};
//            for (String classFile : classFiles) {
//                log.info(classFile);
////                ClassLoader classLoader = new URLClassLoader(urls);
////                Class<?> aClass = classLoader.loadClass(classFile);
////                definedPackages.add(aClass.getClassLoader().getDefinedPackages());
//                definedPackages.addAll(List.of(aClass.getClassLoader().getDefinedPackages()));
//            }
//
//        }
//        return definedPackages;
//    }

    //    public static traverseDir()
    public static StringBuilder getPackages(HashSet<String> set, StringBuilder str, String jar, String rootPath) throws Exception {
        String argLine = argLineLeft + jar + getArgLineRight;

        String path = new File(rootPath).getAbsolutePath();
//        HashSet<Package> definedPackages = getCLasses(rootPath);
        List<String> definedPackages = JavaReadUtil.getClasses(path);
        for (String p : definedPackages) {
            String[] split = p.split("\\.");
            if (split[0].equals("java")) continue;
            if (split.length != 0) {
                str.append(split[0]);
                if (split.length > 1) str.append(".").append(split[1]);
            } else {
                str.append(p);
            }
            str.append(".*");
            set.add(str.toString());
            str.setLength(0);
        }
        str.append(argLine);
        for (String s : set) {
            str.append(s).append(",");
        }
        str.setLength(str.length() - 1);
        str.append(";");
        return str;
    }

    private static void findTargetDirs(File dir) {
        if (dir.isDirectory()) {
            if (dir.getName().equals("target")) {
                System.out.println("Found target dir: " + dir.getAbsolutePath());
                paths.add(dir.getAbsolutePath());
            } else {
                for (File child : dir.listFiles()) {
                    findTargetDirs(child);
                }
            }
        }
    }
}
