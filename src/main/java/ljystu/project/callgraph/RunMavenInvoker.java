package ljystu.project.callgraph;

import ljystu.project.callgraph.entity.Project;
import ljystu.project.callgraph.util.Invoker;
import ljystu.project.callgraph.util.ProjectDownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RunMavenInvoker {
    public static void main(String[] args) throws Exception {
        ProjectDownloader projectDownloader = new ProjectDownloader();
        List<Project> projects = projectDownloader.readProjects("/Users/ljystu/Library/CloudStorage/OneDrive-DelftUniversityofTechnology/thesis/pythonProject/repo_result_per_halfyear_100_test.json");
        Invoker invoker = new Invoker();

        HashSet<String> dependencies = new HashSet<>();
        HashMap<String, Integer> projectCount = new HashMap<>();

        for (Project p : projects) {
            String folderName = projectDownloader.downloadAndUnzip(p);
            if(folderName == "") continue;
            invoker.uploadPackages(folderName, projectCount, "dynamic", dependencies);
        }
//        dependencies


    }
}
