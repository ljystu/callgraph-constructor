package ljystu.project.callgraph;

import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.util.Invoker;
import ljystu.project.callgraph.util.ProjectDownloader;

import java.util.HashMap;
import java.util.List;


public class RunMavenInvoker {
    public static void main(String[] args) {
        ProjectDownloader projectDownloader = new ProjectDownloader();
        List<Project> projects = projectDownloader.readProjects("/Users/ljystu/Library/CloudStorage/OneDrive-DelftUniversityofTechnology/thesis/pythonProject/repo_result_per_halfyear_100_test.json");
        Invoker invoker = new Invoker();

        HashMap<String, Integer> projectCount = new HashMap<>();

        for (Project p : projects) {
            String folderName = projectDownloader.downloadAndUnzip(p);
            if (folderName == "") continue;
            invoker.analyseProject(folderName, projectCount, "dynamic");
        }
//        dependencies


    }
}
