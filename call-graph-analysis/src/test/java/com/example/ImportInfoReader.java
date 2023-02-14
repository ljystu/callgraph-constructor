package com.example;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ImportInfoReader {
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
}