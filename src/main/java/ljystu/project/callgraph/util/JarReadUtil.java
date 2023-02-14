package ljystu.project.callgraph.util;

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
 * The type Java read util.
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
//        HashSet<String> packageNames = new HashSet<>();
        File dir = new File(dirPath);

        if (!dir.isDirectory()) {
            log.error("The given path is not a directory.");
            return classes;
        }

        // search all class files in the dir
        List<File> files = new ArrayList<>();
        findTypeFiles(dir, files, ".class");
        // 正则表达式，用于匹配import语句
//        Pattern importPattern = Pattern.compile("^import\\s(static\\s)?+(.+);$");
//        Pattern packagePattern = Pattern.compile("^package\\s?+(.+);$");

        for (File file : files) {
            classes.add(file.getPath());
//            Path path = file.toPath();
//            String content = "";
//            try {
//                content = Files.readString(path);
//            } catch (IOException e) {
//                e.printStackTrace();
//                continue;
//            }
//
//            String[] lines = content.split("\n");
//            for (String line : lines) {
//
//
//                Matcher importMatcher = importPattern.matcher(line);
//                if (importMatcher.matches()) {
//                    classes.add(importMatcher.group(2));
//                }
//                Matcher packageMatcher = packagePattern.matcher(line);
//                if (packageMatcher.matches()) {
//                    packageNames.add(packageMatcher.group(1) + ".*|");
//                }
//            }
        }
//        for (String pkg : packageNames) {
//            packages.append(pkg);
//        }
//        if (packages.length() > 0) {
//            packages.setLength(packages.length() - 1);
//        }
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
     * @param jarFilePath  the jar file path
     * @param tempDir      the temp dir
     * @param selfPackages the selfPackages
     * @return import info
     */
    public static Set<String> getAllPackages(String jarFilePath, String tempDir, StringBuilder selfPackages) {

        upZipJars(jarFilePath, tempDir);

        Set<String> classes = JarReadUtil.getClasses(tempDir);

        log.info(String.valueOf(classes.size()));

        return getImportedPackages(jarFilePath, classes, selfPackages);
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
     * @param jarFile      the jar file
     * @param classes      the classes
     * @param selfPackages the selfPackages
     * @return the set
     * @throws Exception the exception
     */
    private static Set<String> getImportedPackages(String jarFile, Set<String> classes, StringBuilder selfPackages) {
        Set<String> importedPackages = new HashSet<>();
        HashSet<String> selfPackageNames = new HashSet<>();


        for (String clazz : classes) {
//            Class cls = URLClassLoader.newInstance(new URL[]{url}).loadClass(clazz);

            try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(clazz)))) {
                ClassFile classFile = new ClassFile(dis);
                String name = classFile.getName();

                ClassPool pool = ClassPool.getDefault();
                pool.appendClassPath(jarFile);
                CtClass cc = pool.get(name);

//                getPackageNamesOfParentClasses(cc.getRefClasses(), importedPackages, pool);

                String packageName = cc.getPackageName();
                if (packageName == null) {
                    continue;
                }
                importedPackages.add(packageName);
                if (packageName.lastIndexOf(".") == -1) {
                    selfPackageNames.add(packageName);
                } else {
                    String substring = packageName + ".*|";
                    selfPackageNames.add(substring);
                }
            } catch (IOException | NotFoundException e) {
                log.error(e.toString());
            }
        }

        for (String pkg : selfPackageNames) {
            if (pkg == null) continue;
            selfPackages.append(pkg);
        }
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
