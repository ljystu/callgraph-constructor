package ljystu.project.callgraph.util;

import ljystu.project.callgraph.config.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Invoker.
 */
@Slf4j
public class Invoker {
    public org.apache.maven.shared.invoker.Invoker mavenInvoker = new DefaultInvoker();

//    static String mavenPath = Path.getMavenHome();
//    static String jarPath = Path.getJavaagentHome();

    /**
     * Analyse project hash set.
     *
     * @param rootPath     the root path
     * @param projectCount the project count
     * @param label        the label
     * @return hash set
     */
    public HashSet<String> analyseProject(String rootPath, HashMap<String, Integer> projectCount, String label) {


        HashSet<String> set = new HashSet<>();

        // 获取Test类的所有import的类型
        StringBuilder inclPackages = new StringBuilder();

        PackageUtil packageUtil = new PackageUtil();

        //get path of all pom files
        List<String> pomFiles = packageUtil.getPomFiles(rootPath);

        // TODO 调用dependency:copy store jar into a path
        invokeMavenTask(inclPackages.toString(), rootPath, pomFiles, "dependency:copy-dependencies");
        packageUtil.getPackages(projectCount, set, inclPackages, Path.getJavaagentHome(), rootPath);


        invokeMavenTask(inclPackages.toString(), rootPath, pomFiles, "test");

        // this might be useless now
        HashSet<String> dependencies = new HashSet<>();
        Map<String, String> coordinateMap = getDependencyInfo(rootPath, dependencies);

        File projectDirectory = new File(rootPath).getAbsoluteFile();

//        deleteFile(projectDirectory);

        RedisOp redisOp = new RedisOp();
        redisOp.upload(label, coordinateMap);

        return set;

    }

    /**
     * invoke maven test with given arg
     *
     * @param inclPackages included packages
     * @param path         root path of maven project
     * @param pomFilePaths all pom files in the project
     */
    public void invokeMavenTask(String inclPackages, String path, List<String> pomFilePaths, String task) {

        // 设置Maven的安装目录
        mavenInvoker.setMavenHome(new File(Path.getMavenHome()));
        if (task == "test") {
            POMUtil pomUtil = new POMUtil();
            //add javaagent into surefire configuration of all POM files
            for (String pomFilePath : pomFilePaths) {
                pomUtil.editPOM(pomFilePath, inclPackages);
            }
            invokeTask(path, "test", "");
        } else {
            invokeTask(path, "dependency:copy-dependencies", "./lib");
        }

    }

    /**
     * invoke given maven task
     *
     * @param rootPath roo path
     * @param task     task name
     */
    public void invokeTask(String rootPath, String task, String outputDir) {

        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        String projectMavenFilePath = rootPath + "/pom.xml";
        File projectMavenFile = new File(projectMavenFilePath);
        if (!projectMavenFile.exists()) {
            DeleteUtil.deleteFile(new File(rootPath).getAbsoluteFile());
            return;
        }
        request.setPomFile(projectMavenFile);

        request.setGoals(Collections.singletonList(task));

        request.setJavaHome(new File(Path.getJavaHome()));
        Properties properties = new Properties();
        if (outputDir.length() != 0) {
            properties.setProperty("outputDirectory", outputDir);
        }

        request.setProperties(properties);
        try {
            mavenInvoker.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * Gets dependency info.
     *
     * @param rootPath     the root path
     * @param dependencies the dependencies
     * @return the dependency info
     */
    @Deprecated
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
            if (artifactId.contains("_")) {
                artifactId = artifactId.substring(0, artifactId.indexOf("_"));
            }
            String key = split[0] + ":" + artifactId;
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

        String[] env = new String[]{"JAVA_HOME=" + Path.getJavaHome()};
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd, env, new File(dir)).getInputStream();
             Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }



}
