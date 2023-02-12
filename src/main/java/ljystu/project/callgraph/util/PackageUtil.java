package ljystu.project.callgraph.util;

import ljystu.project.callgraph.config.Arg;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Package util.
 */
@Slf4j
public class PackageUtil {
    /**
     * The Arg line left.
     */
    static String argLineLeft = Arg.getArgLineLeft();
    /**
     * The Arg line right.
     */
    static String argLineRight = Arg.getGetArgLineRight();
    /**
     * The Paths.
     */
    List<String> paths = new ArrayList<>();

    /**
     * Gets pom files.
     *
     * @param rootPath the root path
     * @return the pom files
     */
//    public   HashSet<Package> getCLasses(String rootPath) throws Exception {
//        HashSet<Package> definedPackages = new HashSet<>();
//        Invoker.invoke(new StringBuilder(), rootPath, "compile");
//        findTargetDirs(new File(rootPath));
//        for (String path : paths) {
////            ClassUtil finder = new ClassUtil(new File(path + "/classes/"));
//
//            List<String> classFiles = ClassReadUtil.getClasses(path.subpackageScaning(0,path.lastIndexOf('/')) );
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
    public List<String> getPomFiles(String rootPath) {
        List<File> files = new ArrayList<>();
        ClassReadUtil.findTypeFiles(new File(rootPath), files, "pom.xml");
        List<String> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.getPath());
        }

        return paths;
    }

    /**
     * Gets packages.
     *
     * @param projectCount the project count
     * @param set          the set
     * @param packageScan  the package scan
     * @param javaagent    the javaagent
     * @param rootPath     the root path
     */
    public void getPackages(HashMap<String, Integer> projectCount, Set<String> set, StringBuilder packageScan, String javaagent, String rootPath) {
        String argLine = argLineLeft + javaagent + argLineRight;

        String path = new File(rootPath).getAbsolutePath();
        StringBuilder packagePatternsNames = new StringBuilder();
        Set<String> definedPackages = new HashSet<>();

        List<File> jarFiles = new ArrayList<>();
        ClassReadUtil.findTypeFiles(new File(rootPath + "/lib"), jarFiles, ".jar");
        for (File jar : jarFiles) {
            try {
                definedPackages.addAll(ClassReadUtil.getImportInfo(jar.getAbsolutePath(), rootPath + "/myjar", packagePatternsNames));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DeleteUtil.deleteFile(new File(rootPath + "/myjar"));
            }
        }

        String excludedPackages = readExcludedPackages();
        Pattern excludedPattern = Pattern.compile(excludedPackages);

        Pattern packagePattern = Pattern.compile(packagePatternsNames.toString());

        for (String p : definedPackages) {
            Matcher packageMatcher = packagePattern.matcher(p);
            if (packageMatcher.matches() || getExclPackages(p, excludedPattern)) continue;

            String[] split = p.split("\\.");
            if (split.length != 0) {
                packageScan.append(split[0]);
                if (split.length > 1) packageScan.append(".").append(split[1]);
            } else {
                packageScan.append(p);
            }
            packageScan.append(".*");
            set.add(packageScan.toString());

            packageScan.setLength(0);
        }
        packageScan.append(argLine);
        for (String s : set) {
            projectCount.putIfAbsent(s, 0);
            projectCount.put(s, projectCount.get(s) + 1);
            packageScan.append(s).append(",");
        }
        packageScan.setLength(packageScan.length() - 1);
        packageScan.append(";");
    }

    private String readExcludedPackages() {
        StringBuilder str = new StringBuilder();
        try {
            Path path = new File("src/main/resources/exclusions.txt").toPath();
            String content = Files.readString(path);
            String[] lines = content.split("\r\n");
            for (String line : lines) {
                str.append(line);
                str.append("|");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        str.setLength(str.length() - 1);

        return str.toString();
    }

    private boolean getExclPackages(String definedPackage, Pattern importPattern) {
//        for (String exclusion : exclusionList) {

        Matcher matcher = importPattern.matcher(definedPackage);
        if (matcher.matches()) {
            return true;
        }
//        }
        return false;
    }


    private void findTargetDirs(File dir) {
        if (dir.isDirectory()) {
            if (dir.getName().equals("target")) {
                log.debug("Found target dir: " + dir.getAbsolutePath());
                paths.add(dir.getAbsolutePath());
            } else {
                for (File child : dir.listFiles()) {
                    findTargetDirs(child);
                }
            }
        }
    }
}
