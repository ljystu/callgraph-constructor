package ljystu.project.callgraph.utils;

import ljystu.project.callgraph.config.Constants;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

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
 *
 * @author ljystu
 */
@Slf4j
public class PackageUtil {

    static long tenMegabytes = 10485760L;

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

//    public static Set<String> currentJars = new HashSet<>();

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
    public static String getPackages(String rootPath, String jarName, String coord, String packagePrefix) {

        packageToCoordMap.clear();
        jarToPackageMap.clear();
        jarToCoordMap.clear();
//        currentJars.clear();

        getJarToCoordMap(rootPath, jarName, coord);

        Set<String> inclPackages = extractPackagesFromJar(rootPath, packagePrefix);

        return constructPackageScan(inclPackages, packagePrefix, coord).toString();
    }

    private static StringBuilder constructPackageScan(Set<String> definedPackages, String packagePrefix, String coord) {
        StringBuilder packageScan = new StringBuilder();
        String argLine = Constants.ARG_LINE_LEFT + Constants.JAVAAGENT_HOME + "=";
//                + Constants.ARG_LINE_RIGHT;
        packageScan.append(argLine);
        Pattern excludedPattern = Pattern.compile("^((org\\.junit)|(org\\.junit\\.jupiter)|(org\\.testng)|(org\\.mockito)|(org\\.powermock)|(org\\.easymock)|(org\\.hamcrest)|(org\\.assertj\\.core\\.api)|(io\\.cucumber)|(org\\.spockframework)).*");
//                readExcludedPackages());
        HashSet<String> packagePrefixSet = new HashSet<>();
        for (String definedPackage : definedPackages) {
//            Matcher packageMatcher = selfPackagePattern.matcher(definedPackage);
            if (
//            !packageMatcher.matches() ||
                    isExcluded(definedPackage, excludedPattern)) {
                continue;
            }

            String[] split = definedPackage.split("\\.");
            String prefix;
            if (split.length < 2) {
                prefix = definedPackage;
            } else {
                prefix = split[0] + "." + split[1];
            }
            if (packagePrefixSet.contains(prefix)) {
                continue;
            }
            packagePrefixSet.add(prefix);


            packageScan.append(prefix).append(",");
//                    .append(".*,");

        }

//        packageScan.setLength(packageScan.length() - 1);
//        packageScan.append(";");
        String artifactId = coord.split(":")[1];
        packageScan.
//                append("info=").
        append(packagePrefix).append("!").append(artifactId);
//                .append(";");
        return packageScan;
    }

    private static Set<String> extractPackagesFromJar(String rootPath, String packagePrefix) {
        // find jar files
        List<File> jarFiles = new ArrayList<>();
//        currentJars.clear();
        JarReadUtil.findTypeFiles(new File(rootPath), jarFiles, ".jar");

        Set<String> inclPackages = new HashSet<>();
        for (File jar : jarFiles) {
            try {
                String coord = jarToCoordMap.get(jar.getName());
                if (coord == null) {
                    continue;
                }

                if (jar.length() > tenMegabytes) {
                    log.info(jar.getName() + "Byte value is greater than 10MB");
                    continue;
                }

                if (jarToPackageMap.containsKey(jar.getName())) {
                    inclPackages.addAll(jarToPackageMap.get(jar.getName()));
                    continue;
                }

                extractPackagesToMap(inclPackages, jar, coord, packagePrefix);

//                ProjectUtil.deleteFile(jar);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        for (File jarFile : jarFiles) {
//            ProjectUtil.deleteFile(jarFile);
//        }
        return inclPackages;
    }

    private static void extractPackagesToMap(Set<String> inclPackages, File jar, String coord, String packagePrefix) throws IOException {
        Set<String> packagesInJar = JarReadUtil.getPackages(new JarFile(jar));

        // jar to package
        jarToPackageMap.put(jar.getName(), packagesInJar);

//        currentJars.add(coord);

        for (String importPackage : packagesInJar) {

            if (importPackage.startsWith(packagePrefix)) {
                packageToCoordMap.put(importPackage, coord);
                continue;
            }
            packageToCoordMap.put(importPackage, coord);
        }
        inclPackages.addAll(packagesInJar);

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
    public static void getJarToCoordMap(String rootPath, String jarName, String coord) {

        String dependencyList = execCmd("mvn dependency:list", rootPath);
        if (dependencyList == null) {
            return;
        }
        String[] lines = dependencyList.split("\n");
        Pattern pattern = Pattern.compile("    (.*):(compile|runtime|test)");

        Set<String> dependencies = new HashSet<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String info = matcher.group(1);
                dependencies.add(info);
            }
        }

        log.debug("dependency size:" + dependencies.size());
        jarToCoordMap.put(jarName, coord);

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
            if (split.length != 4) {
                continue;
            }
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
        if (dir == null) {
            return result;
        }
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd, env, new File(dir)).getInputStream(); Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public static void uploadCoordToRedis() {
        Jedis jedis = new Jedis(Constants.SERVER_IP_ADDRESS);
        jedis.auth(Constants.REDIS_PASSWORD);
        for (Map.Entry<String, String> entry : packageToCoordMap.entrySet()) {
            jedis.set(entry.getKey(), entry.getValue());
        }
        jedis.close();
    }
}
