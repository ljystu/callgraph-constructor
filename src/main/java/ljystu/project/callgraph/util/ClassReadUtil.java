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
public class ClassReadUtil {

    private ClassReadUtil() {

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
    static void findTypeFiles(File dir, List<File> list, String type) {

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
     * get all imported packages
     *
     * @param jarFilePath the jar file path
     * @param tempDir     the temp dir
     * @param packages    the packages
     * @return import info
     */
    public static Set<String> getImportInfo(String jarFilePath, String tempDir, StringBuilder packages) {

        //TODO 这两个参数需要 dependency;copy之后直接传入生成的路径

        jarFilePath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";
        tempDir = "src/main/resources/" + "javacg-0.1-SNAPSHOT-dycg-agent";

        extractClasses(jarFilePath, tempDir);

        Set<String> classes = ClassReadUtil.getClasses(tempDir);
        log.info(String.valueOf(classes.size()));

        return getImportedPackages(jarFilePath, classes, packages);
    }

    private static void extractClasses(String jarFilePath, String tempDir) {
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
     * Gets imported packages.
     *
     * @param jarFile  the jar file
     * @param classes  the classes
     * @param packages the packages
     * @return the set
     * @throws Exception the exception
     */
    public static Set<String> getImportedPackages(String jarFile, Set<String> classes, StringBuilder packages) {
        Set<String> importedPackages = new HashSet<>();
        HashSet<String> packageNames = new HashSet<>();

        for (String clazz : classes) {
//            Class cls = URLClassLoader.newInstance(new URL[]{url}).loadClass(clazz);

            try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(clazz)))) {
                ClassFile classFile = new ClassFile(dis);
                String name = classFile.getName();

                ClassPool pool = ClassPool.getDefault();
                pool.appendClassPath(jarFile);
                CtClass cc = pool.get(name);

                getPackageNamesOfParentClasses(cc.getRefClasses(), importedPackages, pool);

                String packageName = cc.getPackageName();
                if (packageName.lastIndexOf(".") == -1) {
                    packageNames.add(packageName);
                } else {
                    String substring = packageName.substring(0, packageName.lastIndexOf(".")) + ".*|";
                    packageNames.add(substring);
                }
            } catch (IOException | NotFoundException e) {
                log.error(e.toString());
            }
        }

        for (String pkg : packageNames) {
            packages.append(pkg);
        }
        if (packages.length() > 0) {
            packages.setLength(packages.length() - 1);
        }
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
            importedPackages.add(importedClassPackageName);
        }
    }
}
