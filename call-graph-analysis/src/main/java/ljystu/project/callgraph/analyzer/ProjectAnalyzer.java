package ljystu.project.callgraph.analyzer;

import eu.fasten.core.data.opal.MavenArtifactDownloader;
import eu.fasten.core.data.opal.MavenCoordinate;
import eu.fasten.core.data.opal.exceptions.MissingArtifactException;
import eu.fasten.core.maven.utils.MavenUtilities;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.utils.PackageUtil;
import ljystu.project.callgraph.utils.ProjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ljystu.project.callgraph.analyzer.OutputGenerator.outputToJson;

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
     *
     * @param projectCount the project count
     * @return hash set
     */
    public List<String> analyseProject(String projectName, Map<String, Integer> projectCount, String dependencyCoordinateWithoutVersion, String tagPrefix, String tagSuffix, String version) {

        //switch tag(maybe need to use commits when there is no tag for smaller projects)
        String switchTagCommand = "git for-each-ref refs/tags --sort=-creatordate --format '%(refname:short)' | head ";
        List<String> projectList = new ArrayList<>();
        List<String> tagNames = getOutput(switchTagCommand, rootPath);

        System.out.println("project name: " + projectName);

        String stashCommand = "git stash";
        HashMap<String, HashMap<String, Object>> analysisResult = new HashMap<>();

        String artifactId = dependencyCoordinateWithoutVersion.split(":")[1];
//        traverse all tags
//        for (String tag : tagNames) {
        String tag = tagPrefix + Constants.VERSION;

        //stash all changes in current branch/tag/commit
        getOutput(stashCommand, rootPath);

        if (!switchTag(tag, rootPath)) {
            System.out.println("switch tag failed");

        }
//        }

//            System.out.println("analy ze tag: " + tag + " start");
//
////            //get specific tag name from git command output
//            if (tag.charAt(tag.length() - 1) == '\'') {
//                tag = tag.substring(0, tag.length() - 1);
//            }
//
//            //version
//            tag = tag.substring(tag.indexOf(tagPrefix) + tagPrefix.length(), tag.length() - tagSuffix.length() + 1);
        tag = Constants.VERSION;
        version = tag;

//
        File jar = null;
        String dependencyCoordinates = dependencyCoordinateWithoutVersion + ":" + tag;

        try {
            //download jar
            jar = new MavenArtifactDownloader(MavenCoordinate.fromString(dependencyCoordinates, "jar")).downloadArtifact(MavenUtilities.MAVEN_CENTRAL_REPO);
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
        String packageScan = PackageUtil.getPackages(rootPath, artifactId + "-" + version + ".jar",
                dependencyCoordinateWithoutVersion + ":" + version, Constants.PACKAGE_PREFIX);

        //upload package:coordinate to redis
        PackageUtil.uploadCoordToRedis();

//            packageScan = Constants.ARG_LINE_LEFT + Constants.JAVAAGENT_HOME;
//            packageScan += "=" + Constants.PACKAGE_PREFIX + "!" + artifactId;
        //javaagent maven test
        HashMap<String, Object> mavenTestWithJavaAgent = mavenTestInvoker.mavenTestWithJavaAgent(packageScan);

        //upload call graph to mongodb
//        CallGraphUploader callGraphUploader = new CallGraphUploader();
//        callGraphUploader.uploadAll(dependencyCoordinateWithoutVersion, artifactId);

        // analysis of call graph in mongo
//        analysisResult.put(version, mongoData(dependencyCoordinateWithoutVersion));
        analysisResult.put("test", mavenTestWithJavaAgent);
        System.out.println("analyse " + projectName + " finished");
//        }


        File file = new File(Constants.PROJECT_FOLDER + artifactId + "-" + version + "/" + projectName + ".json");
        outputToJson(analysisResult, file);

        //delete all files in project folder
        ProjectUtil.deleteFile(new File(rootPath).getAbsoluteFile());
        return projectList;
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
