package ljystu.project.callgraph;

import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.invoker.Invoker;
import ljystu.project.callgraph.util.ProjectUtil;

import java.util.HashMap;
import java.util.List;


public class RunMavenInvoker {
    public static void main(String[] args) {
        ProjectUtil projectUtil = new ProjectUtil();
        List<Project> projects = projectUtil.readProjects("/Users/ljystu/Library/CloudStorage/OneDrive-DelftUniversityofTechnology/thesis/pythonProject/repo_result_per_halfyear_100_test.json");


        HashMap<String, Integer> projectCount = new HashMap<>();

        for (Project p : projects) {

            String folderName = projectUtil.downloadAndUnzip(p);
            if (folderName == "") continue;
            Invoker invoker = new Invoker(folderName);
            invoker.analyseProject(projectCount);

        }
//        dependencies


    }
}
