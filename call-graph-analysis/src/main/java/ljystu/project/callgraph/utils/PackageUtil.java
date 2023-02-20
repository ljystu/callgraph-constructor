package ljystu.project.callgraph.utils;

import ljystu.project.callgraph.config.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Package utils.
 */
@Slf4j
public class PackageUtil {

    /**
     * The Paths.
     */
    List<String> paths = new ArrayList<>();

    static long tenMegabytes = 10485760L; // 10MB的字节数


    /**
     * The constant jarToCoordMap.
     */
    public static Map<String, String> jarToCoordMap = new HashMap<>();
    /**
     * The constant jarToPackageMap.
     */
    public static Map<String, Set<String>> jarToPackageMap = new HashMap<>();
    /**
     * The constant packageToCoordMap.
     */
    public static Map<String, String> packageToCoordMap = new HashMap<>();

    public static Set<String> currentJars = new HashSet<>();

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
     * @param rootPath the root path
     * @return packages packages
     */
    public static String getPackages(String rootPath) {
        getJarToCoordMap(rootPath);

        Set<String> inclPackages = extractPackagesFromJar(rootPath);

        return constructPackageScan(inclPackages).toString();
    }

    private static StringBuilder constructPackageScan(Set<String> definedPackages) {
        StringBuilder packageScan = new StringBuilder();
        String argLine = Constants.argLineLeft + Constants.JAVAAGENT_HOME + Constants.argLineRight;
        packageScan.append(argLine);
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
        return packageScan;
    }

    private static Set<String> extractPackagesFromJar(String rootPath) {
        // find jar files
        List<File> jarFiles = new ArrayList<>();
        currentJars.clear();
        JarReadUtil.findTypeFiles(new File(rootPath), jarFiles, ".jar");


        Set<String> inclPkgs = new HashSet<>();
        for (File jar : jarFiles) {
            try {
                //TODO store the read jars, jar to coordinates and package to coordinates in a databse

                if (jar.length() > tenMegabytes * 2L) {
                    log.info(jar.getName() + "Byte value is greater than 10MB");
                    continue;
                }

                if (jarToPackageMap.containsKey(jar.getName())) {
                    inclPkgs.addAll(jarToPackageMap.get(jar.getName()));
                    continue;
                }
                String coord = jarToCoordMap.get(jar.getName());

                Set<String> packagesInJar = JarReadUtil.getPackages(new JarFile(jar));

                jarToPackageMap.put(jar.getName(), packagesInJar);

                currentJars.add(coord);
                for (String importPackage : packagesInJar) {
                    //TODO 如果packagename相同，后面的coordinate会覆盖当前类的coordinate 需要进一步修改逻辑或添加
                    //可能需要用类名进一步筛选？

                    packageToCoordMap.put(importPackage, coord);
                }
                inclPkgs.addAll(packagesInJar);
                ProjectUtil.deleteFile(jar);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (File jarFile : jarFiles) {
            ProjectUtil.deleteFile(jarFile);
        }
        return inclPkgs;
    }

    private static String readExcludedPackages() {
        StringBuilder str = new StringBuilder();
        try {
            Path path = new File(Constants.EXCLUSION_FILE).toPath();
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
    public static void getJarToCoordMap(String rootPath) {

        String dependencyList = execCmd("mvn dependency:list", rootPath);
        if (dependencyList == null) {
            return;
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

        jarToCoordMap.putAll(extractCoordinate(dependencies));
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
