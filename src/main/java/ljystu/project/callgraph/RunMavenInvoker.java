package ljystu.project.callgraph;

import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.util.Invoker;
import ljystu.project.callgraph.util.ProjectDownloader;

import java.util.List;


public class RunMavenInvoker {
    public static void main(String[] args) throws Exception {
        ProjectDownloader projectDownloader = new ProjectDownloader();
        List<Project> projects = projectDownloader.readProjects("/Users/ljystu/Library/CloudStorage/OneDrive-DelftUniversityofTechnology/thesis/pythonProject/repo_result_per_halfyear_100.json");
        Invoker invoker = new Invoker();
        for (Project p : projects) {
            String folderName = projectDownloader.downloadAndUnzip(p);
            invoker.uploadPackages(folderName);
        }
    }
}
