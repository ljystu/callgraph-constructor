package ljystu.project.callgraph;

import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.invoker.Invoker;
import ljystu.project.callgraph.utils.ProjectUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class RunMavenInvoker {
    public static void main(String[] args) {

        List<Project> projects = ProjectUtil.readProjects(Constants.PROJECT_LIST);

        HashMap<String, Integer> projectCount = new HashMap<>();
        String dependencyCoordinate = "org.apache.zookeeper:zookeeper";
        for (Project p : projects) {
            String folderName = ProjectUtil.gitDownload(p);
            if (Objects.equals(folderName, "")) {
                continue;
            }
            Invoker invoker = new Invoker(folderName);
            invoker.analyseProject(projectCount, dependencyCoordinate);

        }

    }
}
