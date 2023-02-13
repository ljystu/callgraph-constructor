package ljystu.project.callgraph.util;

import ljystu.project.callgraph.config.Arg;
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
    public List<String> getPomFiles(String rootPath) {
        List<File> files = new ArrayList<>();
        JarReadUtil.findTypeFiles(new File(rootPath), files, "pom.xml");
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
     * @return
     */
    public HashMap<String, String> getPackages(HashMap<String, Integer> projectCount, Set<String> set, StringBuilder packageScan, String javaagent, String rootPath) {
        //TODO restructure the code
        String argLine = argLineLeft + javaagent + argLineRight;
        packageScan.append(argLine);

        //possibly useless
        HashSet<String> dependencies = new HashSet<>();


        Map<String, String> jarToCoordMap = getDependencyInfo(rootPath, dependencies);

        StringBuilder selfPackagePatternsNames = new StringBuilder();
        Set<String> definedPackages = new HashSet<>();

        HashMap<String, String> packageToCoordMap = extractPackages(rootPath, jarToCoordMap, selfPackagePatternsNames, definedPackages);

        if (selfPackagePatternsNames.length() > 0) {
            selfPackagePatternsNames.setLength(selfPackagePatternsNames.length() - 1);
        }
        Pattern selfPackagePattern = Pattern.compile(selfPackagePatternsNames.toString());

        constructPackageScan(packageScan, definedPackages, selfPackagePattern);

        return packageToCoordMap;
    }

    private void constructPackageScan(StringBuilder packageScan, Set<String> definedPackages, Pattern selfPackagePattern) {
        Pattern excludedPattern = Pattern.compile(readExcludedPackages());

        for (String definedPackage : definedPackages) {
            Matcher packageMatcher = selfPackagePattern.matcher(definedPackage);
            if (!packageMatcher.matches() || isExcluded(definedPackage, excludedPattern)) {
                continue;
            }
            packageScan.append(definedPackage).append(".*,");

        }

        packageScan.setLength(packageScan.length() - 1);
        packageScan.append(";");
    }

    private static HashMap<String, String> extractPackages(String rootPath, Map<String, String> jarToCoordMap, StringBuilder selfPackagePatternsNames, Set<String> definedPackages) {
        // find jar files
        List<File> jarFiles = new ArrayList<>();
        JarReadUtil.findTypeFiles(new File(rootPath + "/lib"), jarFiles, ".jar");

        HashMap<String, String> packageToCoordMap = new HashMap<>();
        for (File jar : jarFiles) {
            try {
                Set<String> packagesInJar = JarReadUtil.getAllPackages(jar.getAbsolutePath(), rootPath + "/myjar", selfPackagePatternsNames);

                String selfPackages = selfPackagePatternsNames.toString();
                selfPackages = selfPackages.substring(0, selfPackages.length() - 1);
                Pattern selfPackagePatternOfJar = Pattern.compile(selfPackages);

                String coord = jarToCoordMap.get(jar.getName());
                for (String importPackage : packagesInJar) {
                    if (selfPackagePatternOfJar.matcher(importPackage).matches())
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

    private boolean isExcluded(String definedPackage, Pattern importPattern) {
//        for (String exclusion : exclusionList) {

        Matcher matcher = importPattern.matcher(definedPackage);
        if (matcher.matches()) {
            return true;
        }
//        }
        return false;
    }


    /**
     * Gets dependency info.
     *
     * @param rootPath     the root path
     * @param dependencies the dependencies
     * @return the dependency info
     */

    public Map<String, String> getDependencyInfo(String rootPath, Set<String> dependencies) {

        String dependencyList = execCmd("mvn dependency:list", rootPath);
        String[] lines = dependencyList.split("\n");
        Pattern pattern = Pattern.compile("    (.*):(compile|runtime|test)");

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
    @Deprecated
    public Map<String, String> extractCoordinate(Set<String> dependencies) {
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
    public String execCmd(String cmd, String dir) {
        String result = null;

        String[] env = new String[]{"JAVA_HOME=" + ljystu.project.callgraph.config.Path.getJavaHome()};
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd, env, new File(dir)).getInputStream(); Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


}
