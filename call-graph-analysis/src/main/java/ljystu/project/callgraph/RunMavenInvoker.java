package ljystu.project.callgraph;

import ljystu.project.callgraph.analyzer.ProjectAnalyzer;
import ljystu.project.callgraph.config.Constants;

import java.util.Objects;

/**
 * @author ljystu
 */
public class RunMavenInvoker {

    public static void main(String[] args) {

        String projectName = args[0];
        Constants.MAVEN_HOME = args[1];
        Constants.JAVAAGENT_HOME = args[2];
        Constants.JAVA_HOME = args[3];
        Constants.EXCLUSION_FILE = args[4];
        Constants.PROJECT_FOLDER = args[5];
        String dependencyCoordinateWithoutVersion = args[6];

        //tag Prefix and suffix are only needed when analyzing client projects
        String tagPrefix = args[7];
        String tagSuffix = args[8];

        Constants.PACKAGE_PREFIX = args[9];
        Constants.VERSION = args[10];

        //only GroupId:ArtifactId

        String folderName = Constants.PROJECT_FOLDER + projectName;
        //clone project if needed
//                    ProjectUtils.gitDownload(p);

        if (Objects.equals(folderName, "")) {
            System.out.println("Project " + projectName + " not found");
            return;
        }
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(folderName);
        System.out.println("Analyzing project " + projectName);
        projectAnalyzer.analyseProject(projectName, dependencyCoordinateWithoutVersion);

    }
}
