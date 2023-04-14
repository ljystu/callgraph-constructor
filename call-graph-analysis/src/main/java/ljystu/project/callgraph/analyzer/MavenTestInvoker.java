package ljystu.project.callgraph.analyzer;

import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.utils.POMUtil;
import ljystu.project.callgraph.utils.PackageUtil;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static ljystu.project.callgraph.utils.ProjectUtil.deleteFile;

public class MavenTestInvoker {

    String rootPath;
    /**
     * The Maven analyzer.
     */
    private static final org.apache.maven.shared.invoker.Invoker mavenInvoker = new DefaultInvoker();

    public MavenTestInvoker(String rootPath) {
        mavenInvoker.setMavenHome(new File(Constants.MAVEN_HOME));
        this.rootPath = rootPath;
    }


    protected HashMap<String, Object> mavenTestWithJavaAgent(String inclPackages) {
        //get path of all pom files
        List<String> pomFiles = PackageUtil.getPomFiles(rootPath);
        //maven test with javaagent
        addJavaagent(inclPackages, pomFiles);

        return invokeTask("test");
    }

    /**
     * invoke maven test with given arg
     *
     * @param inclPackages included packages
     * @param path         root path of maven project
     * @param pomFilePaths all pom files in the project
     * @param task         the task
     */
    @Deprecated
    public void invokeMavenTask(String inclPackages, String path, List<String> pomFilePaths, String task) {

        // 设置Maven的安装目录

        if (Objects.equals(task, "test")) {
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
        if (request == null) {
            return;
        }
        Properties properties = new Properties();
        if (outputDir.length() != 0) {
            properties.setProperty("outputDirectory", outputDir);
        }
        request.setMavenOpts("-Drat.numUnapprovedLicenses=1000");
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
    public HashMap<String, Object> invokeTask(String task) {
        HashMap<String, Object> testResult = new HashMap<>();
        InvocationRequest request = getInvocationRequest(task);
        if (request == null) {
            return testResult;
        }
        Properties properties = new Properties();

        request.setProperties(properties);
        InvocationResult result = null;
        int totalTests = 0;
        int totalFailures = 0;
        try {
            result = mavenInvoker.execute(request);

            if (result.getExitCode() == 0) {
                // Maven执行成功
                System.out.println("Maven executed successfully.");
                File testReportDir = new File(rootPath, "target/surefire-reports");
                if (testReportDir.exists() && testReportDir.isDirectory()) {
                    File[] testReportFiles = testReportDir.listFiles(file -> file.getName().endsWith(".txt"));
                    if (testReportFiles != null && testReportFiles.length > 0) {

                        for (File testReportFile : testReportFiles) {
                            try (Scanner scanner = new Scanner(testReportFile)) {

                                while (scanner.hasNextLine()) {
                                    String line = scanner.nextLine();
                                    if (line.startsWith("Tests run: ")) {
                                        // 解析测试总数和失败数
                                        String[] parts = line.split(", ");
                                        totalTests = Integer.parseInt(parts[0].substring("Tests run: ".length()));
                                        totalFailures = Integer.parseInt(parts[1].substring("Failures: ".length()));
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.printf(rootPath + ": total tests：%d，failed：%d%n", totalTests, totalFailures);
                    }
                }
            } else {
                // Maven执行失败
                System.err.println("Maven execution failed with exit code: " + result.getExitCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (result != null) {
            testResult.put("exitCode", result.getExitCode());
            testResult.put("totalTests", totalTests);
            testResult.put("totalFailures", totalFailures);
        }

        return testResult;
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

        request.setMavenOpts("-Xverify:none -XX:TieredStopAtLevel=1 -XX:-TieredCompilation");

        return request;
    }
}
