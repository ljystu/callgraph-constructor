package ljystu.project.callgraph;

import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.invoker.Invoker;
import ljystu.project.callgraph.utils.ProjectUtil;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RunMavenInvoker {
    @CommandLine.Option(names = {"-a",
            "--artifact"}, paramLabel = "ARTIFACT")

    static String artifact;

    public static void main(String[] args) {

        //arg[1] is the artifact coordinate
        String filename = args[0];
        Constants.MAVEN_HOME = args[1];
        Constants.JAVAAGENT_HOME = args[2];
        Constants.JAVA_HOME = args[3];
        Constants.EXCLUSION_FILE = args[4];
        Constants.PROJECT_FOLDER = args[5];
        String dependencyCoordinate = args[6];
        String tagPrefix = args[7];
        String tagSuffix = args[8];

        List<Project> projects = ProjectUtil.readProjects(filename);

        HashMap<String, Integer> projectCount = new HashMap<>();
        // do not pass version here only GroupId:ArtifactId

        for (Project p : projects) {
            String folderName = ProjectUtil.gitDownload(p);
            if (Objects.equals(folderName, "")) {
                continue;
            }
            Invoker invoker = new Invoker(folderName);
            invoker.analyseProject(projectCount, dependencyCoordinate, tagPrefix, tagSuffix);

        }

    }
}
