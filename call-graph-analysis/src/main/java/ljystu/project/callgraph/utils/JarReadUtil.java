package ljystu.project.callgraph.utils;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The type Java read utils.
 */
@Slf4j
public class JarReadUtil {

    private JarReadUtil() {

    }


    /**
     * get all class files in the jar
     *
     * @param dirPath the dir path
     * @return classes classes
     */
    public static Set<String> getClasses(String dirPath) {

        HashSet<String> classes = new HashSet<>();
        File dir = new File(dirPath);

        if (!dir.isDirectory()) {
            log.error("The given path is not a directory.");
            return classes;
        }

        // search all class files in the dir
        List<File> files = new ArrayList<>();
        findTypeFiles(dir, files, ".class");
        for (File file : files) {
            classes.add(file.getPath());
        }

        return classes;
    }


    /**
     * Find files of the given type.
     *
     * @param dir  the dir
     * @param list the list
     * @param type the type
     */
    public static void findTypeFiles(File dir, List<File> list, String type) {

        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    findTypeFiles(child, list, type);
                }
            }
        } else if (dir.getName().endsWith(type)) {
            list.add(dir);
        }
    }

    /**
     * get all imported selfPackages
     *
     * @param jarFilePath the jar file path
     * @param tempDir     the temp dir
     * @return import info
     */
    public static Set<String> getAllPackages(String jarFilePath, String tempDir) {

        upZipJars(jarFilePath, tempDir);

        Set<String> classes = JarReadUtil.getClasses(tempDir);

        log.info(String.valueOf(classes.size()));

        return getImportedPackages(jarFilePath, classes);
    }

    private static void upZipJars(String jarFilePath, String tempDir) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets imported selfPackages.
     *
     * @param jarFile the jar file
     * @param classes the classes
     * @return the set
     * @throws Exception the exception
     */
    private static Set<String> getImportedPackages(String jarFile, Set<String> classes) {
        Set<String> importedPackages = new HashSet<>();
//        HashSet<String> selfPackageNames = new HashSet<>();

        ClassPool pool = ClassPool.getDefault();
        ClassPath classPath = null;
        try {
            classPath = pool.appendClassPath(jarFile);
        } catch (NotFoundException e) {
            log.error("ClassPool append error:" + jarFile);
            return importedPackages;
        }

        for (String clazz : classes) {
//            Class cls = URLClassLoader.newInstance(new URL[]{url}).loadClass(clazz);

            try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(clazz)))) {
                ClassFile classFile = new ClassFile(dis);
                String name = classFile.getName();

                CtClass cc = pool.get(name);

//                getPackageNamesOfParentClasses(cc.getRefClasses(), importedPackages, pool);

                String packageName = cc.getPackageName();
                if (packageName == null) {
                    continue;
                }
                importedPackages.add(packageName);
//                if (packageName.lastIndexOf(".") == -1) {
//                    selfPackageNames.add(packageName);
//                } else {
//                    String substring = packageName + ".*|";
//                    selfPackageNames.add(substring);
//                }
                cc.detach();
            } catch (IOException | NotFoundException e) {
                log.error(e.toString());

            }

        }
        if (classPath != null) {
            pool.removeClassPath(classPath);
        }


//        for (String pkg : selfPackageNames) {
//            if (pkg == null) continue;
//            selfPackages.append(pkg);
//        }
//        if (selfPackages.length() > 0) {
//            selfPackages.setLength(selfPackages.length() - 1);
//        }
        return importedPackages;
    }

    private static void getPackageNamesOfParentClasses(Collection<String> refClasses, Set<String> importedPackages, ClassPool pool) {
        for (String refClass : refClasses) {
            CtClass importedClass;
            try {
                importedClass = pool.get(refClass);
            } catch (NotFoundException e) {
                continue;
            }

            String importedClassPackageName = importedClass.getPackageName();
            if (importedClassPackageName == null) {
                continue;
            }
            importedPackages.add(importedClassPackageName);
        }
    }
}
