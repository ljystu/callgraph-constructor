package ljystu.project.callgraph;

import ljystu.project.callgraph.analyzer.ProjectAnalyzer;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.utils.ProjectUtil;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author ljystu
 */
public class RunMavenInvoker {
    @CommandLine.Option(names = {"-a",
            "--artifact"}, paramLabel = "ARTIFACT")

    static String artifact;

    public static void main(String[] args) {

        String filename = args[0];
        Constants.MAVEN_HOME = args[1];
        Constants.JAVAAGENT_HOME = args[2];
        Constants.JAVA_HOME = args[3];
        Constants.EXCLUSION_FILE = args[4];
        Constants.PROJECT_FOLDER = args[5];
        String dependencyCoordinateWithoutVersion = args[6];
        String tagPrefix = args[7];
        String tagSuffix = args[8];
        Constants.PACKAGE_PREFIX = args[9];
        Constants.VERSION = args[10];

        List<Project> projects = ProjectUtil.readProjects(filename);

        String version = filename.substring(filename.lastIndexOf(":") + 1, filename.lastIndexOf("."));

        HashMap<String, Integer> projectCount = new HashMap<>();
        // do not pass version here only GroupId:ArtifactId

        for (Project p : projects) {
            String folderName = Constants.PROJECT_FOLDER + p.getName();
//                    ProjectUtil.gitDownload(p);
            if (Objects.equals(folderName, "")) {
                continue;
            }
            ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(folderName);
            System.out.println("Analyzing project " + p.getName());
            projectAnalyzer.analyseProject(p.getName(), projectCount, dependencyCoordinateWithoutVersion, tagPrefix, tagSuffix, version);

        }

    }
}
