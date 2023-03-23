package ljystu.project.callgraph.invoker;

import com.alibaba.fastjson.JSON;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import eu.fasten.analyzer.javacgopal.Main;
import eu.fasten.core.data.opal.MavenArtifactDownloader;
import eu.fasten.core.data.opal.MavenCoordinate;
import eu.fasten.core.data.opal.exceptions.MissingArtifactException;
import eu.fasten.core.maven.utils.MavenUtilities;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.myEdge;
import ljystu.project.callgraph.uploader.CallGraphUploader;
import ljystu.project.callgraph.utils.POMUtil;
import ljystu.project.callgraph.utils.PackageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    public Set<String> analyseProject(Map<String, Integer> projectCount, String dependencyCoordinateWithoutVersion, String tagPrefix, String tagSuffix) {

        //switch tag(maybe need to use commits when there is no tag for smaller projects)
        String switchTagCommand =
                "git for-each-ref refs/tags --sort=-taggerdate --format '%(refname:short)' | head ";
        HashSet<String> set = new HashSet<>();
        List<String> tagNames = getOutput(switchTagCommand, rootPath);

        System.out.println("tagNames: " + tagNames);

        String stashCommand = "git stash";
        //traverse all tags
        for (String tag : tagNames) {

            getOutput(stashCommand, rootPath);

            if (!switchTag(tag, rootPath)) {
                System.out.println("switch tag failed");
                continue;
            }
            System.out.println("analyze tag: " + tag + " start");

            //get specific tag name from git command output
            if (tag.charAt(tag.length() - 1) == '\'') {
                tag = tag.substring(0, tag.length() - 1);
            }
            tag = tag.substring(tag.indexOf(tagPrefix) + tagPrefix.length(), tag.length() - tagSuffix.length() + 1);


            String artifactId = dependencyCoordinateWithoutVersion.split(":")[1];
            File jar = null;
            String dependencyCoordinates = dependencyCoordinateWithoutVersion + ":" + tag;

            try {
                jar = new MavenArtifactDownloader(MavenCoordinate.fromString(dependencyCoordinates, "jar")).downloadArtifact(MavenUtilities.MAVEN_CENTRAL_REPO);
                // 复制文件到目标文件夹
                Path targetDirectory = Paths.get(rootPath);

                Files.copy(jar.toPath(), targetDirectory.resolve(artifactId + "-" + tag + ".jar"));
                System.out.println("File copied successfully!");
            } catch (MissingArtifactException | IOException e) {
                log.info("Artifact not found: " + dependencyCoordinateWithoutVersion + ":" + tag);
                continue;
            } finally {
                if (jar != null) {
                    jar.delete();
                }
            }

            //acquire dependencies
            invokeTask("dependency:copy-dependencies", "./lib");

            //map packages to coordinates
            String packageScan = PackageUtil.getPackages(rootPath, artifactId + "-" + tag + ".jar", dependencyCoordinates);

            //javaagent maven test
            mavenTestWithJavaAgent(packageScan);

            //upload package:coordinate to redis
            PackageUtil.uploadCoordToRedis();

            //upload call graph to mongodb
            CallGraphUploader callGraphUploader = new CallGraphUploader();

            callGraphUploader.uploadAll(dependencyCoordinateWithoutVersion);

//            constructStaticCallGraphs();
            Main.main(getParameters(dependencyCoordinates));

            // analysis of call graph in mongo
            mongoData(dependencyCoordinateWithoutVersion);

            System.out.println("analyse " + tag + " finished");
        }

//        ProjectUtil.deleteFile(new File(rootPath).getAbsoluteFile());

        return set;
    }

    public void mongoData(String dependencyCoordinate) {
        ServerAddress serverAddress = new ServerAddress(Constants.REDIS_ADDRESS, 27017);
        List<ServerAddress> addrs = new ArrayList<ServerAddress>();
        addrs.add(serverAddress);

        MongoCredential credential = MongoCredential.createScramSha1Credential("ljystu", "admin", "Ljystu110!".toCharArray());
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        credentials.add(credential);

        //通过连接认证获取MongoDB连接
        MongoClient mongoClient = new MongoClient(addrs, credentials);

        // 获取MongoDB数据库
        MongoDatabase database = mongoClient.getDatabase("mydatabase");

        // 获取MongoDB集合
        MongoCollection<Document> collection = database.getCollection(dependencyCoordinate);

        int staticCount = 0;
        int dynamicCount = 0;
        int bothCount = 0;
        int internalDynamicCall = 0;
        int externalDynamicCall = 0;
        int dynCalled = 0;
        int dynCalling = 0;

        HashMap<String, Integer> dynamicCoordinates = new HashMap<>();
        HashMap<String, Integer> staticCoordinates = new HashMap<>();
        HashSet<myEdge> edges = new HashSet<>();
        HashSet<String> staticCoords = new HashSet<>();
        HashMap<String, Integer> bothCoordinates = new HashMap<>();

        for (Document document : collection.find()) {
            myEdge edge = JSON.parseObject(document.toJson(), myEdge.class);

            String endCoordinate = edge.getEndNode().getCoordinate();
            String startCoordinate = edge.getStartNode().getCoordinate();

            edges.add(edge);
            if (edge.getType().equals("static")) {
                staticCount++;
                if (startCoordinate.startsWith(dependencyCoordinate)) {
                    staticCoordinates.put(startCoordinate, staticCoordinates.getOrDefault(startCoordinate, 0) + 1);
                }
                staticCoords.add(startCoordinate);

            } else if (edge.getType().equals("dynamic")) {
                dynamicCount++;
                if (startCoordinate.startsWith(dependencyCoordinate) && endCoordinate.startsWith(dependencyCoordinate)) {
                    internalDynamicCall++;
                    dynamicCoordinates.put(startCoordinate, dynamicCoordinates.getOrDefault(startCoordinate, 0) + 1);
                } else {
                    externalDynamicCall++;
                    if (startCoordinate.startsWith(dependencyCoordinate)) {
                        dynamicCoordinates.put(startCoordinate, dynamicCoordinates.getOrDefault(startCoordinate, 0) + 1);
                        dynCalling++;
                    } else {
                        dynamicCoordinates.put(startCoordinate, dynamicCoordinates.getOrDefault(startCoordinate, 0) + 1);
                        dynCalled++;
                    }
                }
            } else {
                bothCount++;
                if (startCoordinate.startsWith(dependencyCoordinate) || endCoordinate.startsWith(dependencyCoordinate)) {
                    bothCoordinates.put(startCoordinate, bothCoordinates.getOrDefault(startCoordinate, 0) + 1);
                }
            }
        }
        System.out.println("staticCount = " + staticCount);
        System.out.println("dynamicCount = " + dynamicCount);
        System.out.println("internalDynCount = " + internalDynamicCall);
        System.out.println("externalDynCount = " + externalDynamicCall);
        System.out.println("dynCalled = " + dynCalled);
        System.out.println("dynCalling = " + dynCalling);
        System.out.println("bothCount = " + bothCount);

        for (Map.Entry<String, Integer> entry : dynamicCoordinates.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("static");
        for (Map.Entry<String, Integer> entry : staticCoordinates.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        // 关闭MongoClient
        mongoClient.close();

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
    public void invokeTask(String task) {

        InvocationRequest request = getInvocationRequest(task);
        if (request == null) {
            return;
        }
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

    private static boolean switchTag(String tagName, String path) {

        try {

            if (tagName == null) {
                return false;
            }

            System.out.println("tag: " + tagName);

            String switchCommand = "git checkout " + tagName.substring(1, tagName.length() - 1);
            Process switchProcess = Runtime.getRuntime().exec(switchCommand, null, new File(path));
//        ProcessBuilder processBuilder = new ProcessBuilder(switchCommand);
//        processBuilder.directory(new File(path));
//        Process switchProcess = processBuilder.start();
            boolean switched = switchProcess.waitFor(5, TimeUnit.MINUTES);
            if (!switched) {
                switchProcess.destroy();
                return false;
            }
            System.out.println("switched to " + tagName);
            switchProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static List<String> getOutput(String describeCommand, String path) {
        List<String> describeOutput = new ArrayList<>();
        try {
            Process describeProcess = Runtime.getRuntime().exec(describeCommand, null, new File(path));

            boolean b = describeProcess.waitFor(3, TimeUnit.MINUTES);
            if (!b) {
                describeProcess.destroy();
                return null;
            }
            BufferedReader describeInput = new BufferedReader(new InputStreamReader(describeProcess.getInputStream()));
            String line;
            while ((line = describeInput.readLine()) != null) {
                if (line.contains("fatal") || line.contains("error")) {
                    break;
                }
                describeOutput.add(line);
                if (describeOutput.size() == 15) {
                    break;
                }
            }
            describeInput.close();
            describeProcess.destroy();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return describeOutput;
    }


}
