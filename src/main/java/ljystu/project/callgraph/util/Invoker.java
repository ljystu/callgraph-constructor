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

@Slf4j
public class Invoker {
    static org.apache.maven.shared.invoker.Invoker mavenInvoker = new DefaultInvoker();

//    static String mavenPath = Path.getMavenHome();
//    static String jarPath = Path.getJavaagentHome();

    public HashSet<String> analyseProject(String rootPath, HashMap<String, Integer> projectCount, String label) {


        HashSet<String> set = new HashSet<>();

        // 获取Test类的所有import的类型
        StringBuilder str = new StringBuilder();

        PackageUtil packageUtil = new PackageUtil();

        packageUtil.getPackages(projectCount, set, str, Path.getJavaagentHome(), rootPath);

        List<String> pomFiles = packageUtil.getPomFiles(rootPath);

        invokeTask(str, rootPath, pomFiles);

        HashSet<String> dependencies = new HashSet<>();
        Map<String, String> coordinateMap = getDependencyInfo(rootPath, dependencies);

        File projectDirectory = new File(rootPath).getAbsoluteFile();

//        deleteFile(projectDirectory);

        RedisOp redisOp = new RedisOp();
        redisOp.upload(label, coordinateMap);

        return set;

    }


    public void invokeTask(StringBuilder str, String path, List<String> pomFilePaths) {

        // 设置Maven的安装目录
        mavenInvoker.setMavenHome(new File(Path.getMavenHome()));
        POMUtil pomUtil = new POMUtil();
        for (String pomFilePath : pomFilePaths) {
            pomUtil.editPOM(pomFilePath, str.toString());
        }
        invoke(path, "test");

    }

    public void invoke(String rootPath, String task) {

        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        String projectMavenFilePath = rootPath + "/pom.xml";
        File projectMavenFile = new File(projectMavenFilePath);
        if (!projectMavenFile.exists()) {
            deleteFile(new File(rootPath).getAbsoluteFile());
            return;
        }
        request.setPomFile(projectMavenFile);

        request.setGoals(Collections.singletonList(task));

        request.setJavaHome(new File(Path.getJavaHome()));
//        Properties properties = new Properties();
//        if (str.length() != 0) {
//            properties.setProperty("argLine", str.toString() + "excl=org.maven.wagon.*;" );
//        }
//        request.setMavenOpts(str.toString());
//        request.setProperties(properties);
        try {
            mavenInvoker.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

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

    public void deleteFile(File dirFile) {
        // 如果dir对应的文件不存在，则退出
        if (!dirFile.exists()) {
            return;
        }

        if (dirFile.isFile()) {
            dirFile.delete();
            return;
        } else {

            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }

        dirFile.delete();
    }

}
