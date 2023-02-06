package ljystu.project.callgraph.util;

import ljystu.project.callgraph.config.Arg;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypher.internal.expressions.In;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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


    public void getPackages(HashMap<String, Integer> projectCount, Set<String> set, StringBuilder packageScan, String jar, String rootPath) throws Exception {
        String argLine = argLineLeft + jar + argLineRight;

        String path = new File(rootPath).getAbsolutePath();
//        HashSet<Package> definedPackages = getCLasses(rootPath);
        List<String> definedPackages = JavaReadUtil.getClasses(path);
        for (String p : definedPackages) {
            String[] split = p.split("\\.");
            if (split.length != 0) {
                if (split[0].equals("java")) continue;
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
