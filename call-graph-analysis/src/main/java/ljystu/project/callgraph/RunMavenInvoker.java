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
        ProjectUtil projectUtil = new ProjectUtil();
        List<Project> projects = projectUtil.readProjects(Constants.PROJECT_LIST);

        HashMap<String, Integer> projectCount = new HashMap<>();

        for (Project p : projects) {
            String folderName = projectUtil.downloadAndUnzip(p);
            if (Objects.equals(folderName, "")) continue;
            Invoker invoker = new Invoker(folderName);
            invoker.analyseProject(projectCount);

        }
//        dependencies


    }
}
