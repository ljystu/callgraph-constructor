package ljystu.project.callgraph.util;

import ljystu.project.callgraph.config.Arg;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PackageUtil {
    static String argLineLeft = Arg.getArgLineLeft();
    static String argLineRight = Arg.getGetArgLineRight();
    List<String> paths = new ArrayList<>();

    //    public   HashSet<Package> getCLasses(String rootPath) throws Exception {
//        HashSet<Package> definedPackages = new HashSet<>();
//        Invoker.invoke(new StringBuilder(), rootPath, "compile");
//        findTargetDirs(new File(rootPath));
//        for (String path : paths) {
////            ClassUtil finder = new ClassUtil(new File(path + "/classes/"));
//
//            List<String> classFiles = JavaReadUtil.getClasses(path.subpackageScaning(0,path.lastIndexOf('/')) );
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
        JavaReadUtil.findClassFiles(new File(rootPath), files, "pom.xml");
        List<String> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.getPath());
        }

        return paths;
    }

    public void getPackages(HashMap<String, Integer> projectCount, Set<String> set, StringBuilder packageScan, String jar, String rootPath) {
        String argLine = argLineLeft + jar + argLineRight;

        String path = new File(rootPath).getAbsolutePath();

        HashSet<String> definedPackages = JavaReadUtil.getClasses(path);
        String excludedPackages = readExcludedPackages();

        for (String p : definedPackages) {
            if (getInclPackages(p, excludedPackages)) continue;
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

    private boolean getInclPackages(String definedPackage, String exclustionList) {
//        for (String exclusion : exclustionList) {
        Pattern importPattern = Pattern.compile(exclustionList);
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
