package ljystu.project.callgraph.invoker;

import ljystu.project.callgraph.config.Path;
import ljystu.project.callgraph.redis.RedisOp;
import ljystu.project.callgraph.util.POMUtil;
import ljystu.project.callgraph.util.PackageUtil;
import ljystu.project.callgraph.util.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.util.*;

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

        invokeMavenTask(inclPackages.toString(), rootPath, pomFiles, "dependency:copy-dependencies");

        HashMap<String, String> packageToCoordMap = packageUtil.getPackages(projectCount, set, inclPackages, Path.getJavaagentHome(), rootPath);


        invokeMavenTask(inclPackages.toString(), rootPath, pomFiles, "test");


        File projectDirectory = new File(rootPath).getAbsoluteFile();

//        deleteFile(projectDirectory);

        RedisOp redisOp = new RedisOp();
        redisOp.upload(label, packageToCoordMap);

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
            invokeTask(path, "test");
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

        InvocationRequest request = getInvocationRequest(rootPath, task);
        if (request == null) return;
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

    public void invokeTask(String rootPath, String task) {

        InvocationRequest request = getInvocationRequest(rootPath, task);
        if (request == null) return;
        Properties properties = new Properties();

        request.setProperties(properties);
        try {
            mavenInvoker.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static InvocationRequest getInvocationRequest(String rootPath, String task) {
        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        String projectMavenFilePath = rootPath + "/pom.xml";
        File projectMavenFile = new File(projectMavenFilePath);
        if (!projectMavenFile.exists()) {
            ProjectUtil.deleteFile(new File(rootPath).getAbsoluteFile());
            return null;
        }
        request.setPomFile(projectMavenFile);

        request.setGoals(Collections.singletonList(task));

        request.setJavaHome(new File(Path.getJavaHome()));
        return request;
    }


}
