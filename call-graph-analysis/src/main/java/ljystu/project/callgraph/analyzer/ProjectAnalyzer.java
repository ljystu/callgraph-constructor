package ljystu.project.callgraph.analyzer;

import eu.fasten.core.data.opal.MavenArtifactDownloader;
import eu.fasten.core.data.opal.MavenCoordinate;
import eu.fasten.core.data.opal.exceptions.MissingArtifactException;
import eu.fasten.core.maven.utils.MavenUtilities;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.uploader.CallGraphUploader;
import ljystu.project.callgraph.utils.PackageUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The type ProjectAnalyzer.
 *
 * @author ljystu
 */
@Slf4j
public class ProjectAnalyzer {
    private final String rootPath;

    private MavenTestInvoker mavenTestInvoker;

    /**
     * Instantiates a new ProjectAnalyzer.
     *
     * @param rootPath the root path
     */
    public ProjectAnalyzer(String rootPath) {
        mavenTestInvoker = new MavenTestInvoker(rootPath);
        this.rootPath = rootPath;
    }

    /**
     * Analyse project hash set.
     */
    public void analyseProject(String projectName, String dependencyCoordinateWithoutVersion) {

        //switch tag(maybe need to use commits when there is no tag for smaller projects)
//        String switchTagCommand = "git for-each-ref refs/tags --sort=-creatordate --format '%(refname:short)' | head ";
        List<String> projectList = new ArrayList<>();
//        List<String> tagNames = getOutput(switchTagCommand, rootPath);

        System.out.println("project name: " + projectName);

        String artifactId = dependencyCoordinateWithoutVersion.split(":")[1];

        String tag = Constants.VERSION;

        File jar = null;
        String dependencyCoordinate = dependencyCoordinateWithoutVersion + ":" + tag;

        try {
            //download jar
            jar = new MavenArtifactDownloader(MavenCoordinate.fromString(dependencyCoordinate, "jar")).downloadArtifact(MavenUtilities.MAVEN_CENTRAL_REPO);
            //copy jar to lib
            Path targetDirectory = Paths.get(rootPath);
            Files.copy(jar.toPath(), targetDirectory.resolve(artifactId + "-" + tag + ".jar"));

            System.out.println("File copied successfully!");
        } catch (MissingArtifactException | IOException e) {
            log.info("Artifact not found: " + dependencyCoordinateWithoutVersion + ":" + tag);
//                continue;
        } finally {
            if (jar != null) {
                jar.delete();
            }
        }

        //acquire dependencies
        mavenTestInvoker.invokeTask("dependency:copy-dependencies", "./lib");
        artifactId = dependencyCoordinateWithoutVersion.split(":")[1];


        //map packages to coordinates
        String packageScan = PackageUtils.getPackages(rootPath, artifactId + "-" + tag + ".jar",
                dependencyCoordinate, Constants.PACKAGE_PREFIX);

        //upload package:coordinate to redis
        PackageUtils.uploadCoordToRedis(dependencyCoordinate);

        //javaagent maven test with packages needed
        HashMap<String, Object> mavenTestWithJavaAgent = mavenTestInvoker.mavenTestWithJavaAgent(packageScan);

        //upload call graph to mongodb
        CallGraphUploader callGraphUploader = new CallGraphUploader();
        callGraphUploader.uploadAll(dependencyCoordinate, artifactId);


//        }

        // analysis of call graph in mongo
//        analysisResult.put(version, mongoData(dependencyCoordinate));
//        analysisResult.put("test", mavenTestWithJavaAgent);
//        System.out.println("analyze " + projectName + " finished");
//        File file = new File(Constants.PROJECT_FOLDER + "outputjson/" + projectName + "/" + artifactId + "-" + version + ".json");
//        outputToJson(analysisResult, file);

        //delete all files in project folder
//        ProjectUtils.deleteFile(new File(rootPath).getAbsoluteFile());
    }

    private String[] getParameters(String coordinate) {
        return new String[]{"-a", coordinate, "-an", coordinate, "-g", "-i", "COORD", "-m"};
    }

    private static boolean switchTag(String tagName, String path) {

        try {

            if (tagName == null) {
                return false;
            }

            System.out.println("tag: " + tagName);

            String switchCommand = "git checkout " + tagName;
            Process switchProcess = Runtime.getRuntime().exec(switchCommand, null, new File(path));

            boolean switched = switchProcess.waitFor(5, TimeUnit.MINUTES);
            if (!switched) {
                switchProcess.destroy();
                return false;
            }
            System.out.println("switched to " + tagName);
            switchProcess.destroy();
        } catch (InterruptedException | IOException e) {
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
                return describeOutput;
            }
            BufferedReader describeInput = new BufferedReader(new InputStreamReader(describeProcess.getInputStream()));
            String line;
            while ((line = describeInput.readLine()) != null) {
                if (line.contains("fatal") || line.contains("error")) {
                    break;
                }
                describeOutput.add(line);
                if (describeOutput.size() == 1) {
                    break;
                }
            }
            describeInput.close();
            describeProcess.destroy();

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return describeOutput;
    }


}
