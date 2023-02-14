package ljystu.project.callgraph.util;

import ljystu.project.callgraph.config.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
     * The Paths.
     */
    List<String> paths = new ArrayList<>();

    /**
     * Gets pom files.
     *
     * @param rootPath the root path
     * @return the pom files
     */
    public static List<String> getPomFiles(String rootPath) {
        List<File> files = new ArrayList<>();
        JarReadUtil.findTypeFiles(new File(rootPath), files, "pom.xml");
        List<String> pomFiles = new ArrayList<>();
        for (File file : files) {
            pomFiles.add(file.getPath());
        }

        return pomFiles;
    }

    /**
     * Gets packages.
     *
     * @param jarToCoordMap the jar to coord map
     * @param packageScan   the package scan
     * @param rootPath      the root path
     * @return packages packages
     */
    public static Map<String, String> getPackages(Map<String, String> jarToCoordMap, StringBuilder packageScan, String rootPath) {

        String argLine = Constants.argLineLeft + Constants.JAVAAGENT_HOME + Constants.argLineRight;
        packageScan.append(argLine);

        //possibly useless
//        HashSet<String> dependencies = new HashSet<>();

        jarToCoordMap.putAll(getJarToCoordMap(rootPath));

        Set<String> definedPackages = new HashSet<>();

        HashMap<String, String> packageToCoordMap = extractPackagesFromJar(rootPath, jarToCoordMap, definedPackages);

        constructPackageScan(packageScan, definedPackages);

        return packageToCoordMap;
    }

    private static void constructPackageScan(StringBuilder packageScan, Set<String> definedPackages) {
        Pattern excludedPattern = Pattern.compile(readExcludedPackages());

        for (String definedPackage : definedPackages) {
//            Matcher packageMatcher = selfPackagePattern.matcher(definedPackage);
            if (
//            !packageMatcher.matches() ||
                    isExcluded(definedPackage, excludedPattern)) {
                continue;
            }
            packageScan.append(definedPackage).append(".*,");

        }

        packageScan.setLength(packageScan.length() - 1);
        packageScan.append(";");
    }

    private static HashMap<String, String> extractPackagesFromJar(String rootPath, Map<String, String> jarToCoordMap, Set<String> definedPackages) {
        // find jar files
        List<File> jarFiles = new ArrayList<>();
        JarReadUtil.findTypeFiles(new File(rootPath), jarFiles, ".jar");

        HashMap<String, String> packageToCoordMap = new HashMap<>();
        for (File jar : jarFiles) {
            try {
                Set<String> packagesInJar = JarReadUtil.getAllPackages(jar.getAbsolutePath(), rootPath + "/myjar");

                String coord = jarToCoordMap.get(jar.getName());
                for (String importPackage : packagesInJar) {
                    //TODO 如果packagename相同，后面的coordinate会覆盖当前类的coordinate 需要进一步修改逻辑或添加
                    //可能需要用类名进一步筛选？
                    packageToCoordMap.put(importPackage, coord);
                }
                definedPackages.addAll(packagesInJar);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ProjectUtil.deleteFile(new File(rootPath + "/myjar"));
            }
        }
        return packageToCoordMap;
    }

    private static String readExcludedPackages() {
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

    private static boolean isExcluded(String definedPackage, Pattern importPattern) {
        Matcher matcher = importPattern.matcher(definedPackage);
        return matcher.matches();
    }


    /**
     * Gets dependency info.
     *
     * @param rootPath the root path
     * @return the dependency info
     */
    public static Map<String, String> getJarToCoordMap(String rootPath) {

        String dependencyList = execCmd("mvn dependency:list", rootPath);
        if (dependencyList == null) {
            return new HashMap<>();
        }
        String[] lines = dependencyList.split("\n");
        Pattern pattern = Pattern.compile("    (.*):(compile|runtime|test)");

        Set<String> dependencies = new HashSet<>();
        for (String line : lines) {
            if (line == null) continue;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String info = matcher.group(1);
                dependencies.add(info);
            }
        }

        log.debug("dependency size:" + dependencies.size());

        return extractCoordinate(dependencies);
    }

    /**
     * Extract coordinate map.
     *
     * @param dependencies the dependencies
     * @return the map
     */
    public static Map<String, String> extractCoordinate(Set<String> dependencies) {
        HashMap<String, String> coordinateMap = new HashMap<>();
        for (String dependency : dependencies) {
            String[] split = dependency.split(":");
            if (split.length != 4) continue;
            String artifactId = split[1];
//            if (artifactId.contains("_")) {
//                artifactId = artifactId.substring(0, artifactId.indexOf("_"));
//            }
            String key = artifactId + "-" + split[3] + ".jar";
            String coordinate = split[0] + ":" + artifactId + ":" + split[3];
            coordinateMap.put(key, coordinate);
        }

        return coordinateMap;
    }


    /**
     * Exec cmd string.
     *
     * @param cmd the cmd
     * @param dir the dir
     * @return the string
     */
    public static String execCmd(String cmd, String dir) {
        String result = null;

        String[] env = new String[]{"JAVA_HOME=" + Constants.JAVA_HOME};
        if (dir == null) return result;
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd, env, new File(dir)).getInputStream(); Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


}
