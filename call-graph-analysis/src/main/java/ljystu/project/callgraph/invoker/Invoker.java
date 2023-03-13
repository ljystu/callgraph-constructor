package ljystu.project.callgraph.invoker;

import eu.fasten.analyzer.javacgopal.Main;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.uploader.CallGraphUploader;
import ljystu.project.callgraph.utils.POMUtil;
import ljystu.project.callgraph.utils.PackageUtil;
import ljystu.project.callgraph.utils.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.util.*;

import static ljystu.project.callgraph.utils.ProjectUtil.deleteFile;

/**
 * The type Invoker.
 */
@Slf4j
public class Invoker {
    /**
     * The Maven invoker.
     */
    private static final org.apache.maven.shared.invoker.Invoker mavenInvoker = new DefaultInvoker();
    ;
    private final String rootPath;

    /**
     * Instantiates a new Invoker.
     *
     * @param rootPath the root path
     */
    public Invoker(String rootPath) {
        mavenInvoker.setMavenHome(new File(Constants.MAVEN_HOME));
        this.rootPath = rootPath;
    }

    //    static String mavenPath = Path.getMavenHome();
//    static String jarPath = Path.getJavaagentHome();

    /**
     * Analyse project hash set.
     *
     * @param projectCount the project count
     * @return hash set
     */
    public Set<String> analyseProject(Map<String, Integer> projectCount) {

        HashSet<String> set = new HashSet<>();

        //acquire dependencies
        invokeTask("dependency:copy-dependencies", "./lib");
        //map packages to coordinates
        String packageScan = PackageUtil.getPackages(rootPath);

        mavenTestWithJavaAgent(packageScan);

        ProjectUtil.deleteFile(new File(rootPath).getAbsoluteFile());

        constructStaticCallGraphs();

        //upload call graph to neo4j
        CallGraphUploader callGraphUploader = new CallGraphUploader();

//        callGraphUploader.uploadAll();


        return set;
    }

    private void constructStaticCallGraphs() {
        for (String coordinate : PackageUtil.currentJars) {
            Main.main(getParameters(coordinate));

        }
    }

    private void mavenTestWithJavaAgent(String inclPackages) {
        //get path of all pom files
        List<String> pomFiles = PackageUtil.getPomFiles(rootPath);
        //maven test with javaagent
        addJavaagent(inclPackages, pomFiles);
        invokeTask("test");
    }

    private String[] getParameters(String coordinate) {
        return new String[]{"-a", coordinate, "-an", coordinate, "-g", "-i", "COORD", "-m"};
    }

    /**
     * invoke maven test with given arg
     *
     * @param inclPackages included packages
     * @param path         root path of maven project
     * @param pomFilePaths all pom files in the project
     * @param task         the task
     */
    public void invokeMavenTask(String inclPackages, String path, List<String> pomFilePaths, String task) {

        // 设置Maven的安装目录

        if (task == "test") {
            addJavaagent(inclPackages, pomFilePaths);
            invokeTask(path, "test");
        }

    }

    private void addJavaagent(String inclPackages, List<String> pomFilePaths) {

        //add javaagent into surefire configuration of all POM files
        for (String pomFilePath : pomFilePaths) {
            POMUtil.editPOM(pomFilePath, inclPackages);
        }
    }

    /**
     * invoke given maven task
     *
     * @param task      task name
     * @param outputDir the output dir
     */
    public void invokeTask(String task, String outputDir) {

        InvocationRequest request = getInvocationRequest(task);
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

    /**
     * Invoke task.
     *
     * @param task the task
     */
    public void invokeTask(String task) {

        InvocationRequest request = getInvocationRequest(task);
        if (request == null) return;
        Properties properties = new Properties();

        request.setProperties(properties);
        try {
            mavenInvoker.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private InvocationRequest getInvocationRequest(String task) {
        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        String projectMavenFilePath = rootPath + "/pom.xml";
        File projectMavenFile = new File(projectMavenFilePath);
        if (!projectMavenFile.exists()) {
            deleteFile(new File(rootPath).getAbsoluteFile());
            return null;
        }
        request.setPomFile(projectMavenFile);

        request.setGoals(Collections.singletonList(task));

        request.setJavaHome(new File(Constants.JAVA_HOME));
        return request;
    }


}
