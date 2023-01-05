package com.example.neo4jtest;

import com.example.neo4jtest.entity.Project;
import com.example.neo4jtest.util.ProjectDownloader;

import java.util.List;

import static com.example.neo4jtest.util.invokerUtil.uploadPackages;

public class runMavenInvoker {
    public static void main(String[] args) throws Exception {
        ProjectDownloader projectDownloader = new ProjectDownloader();
        List<Project> projects = projectDownloader.readProjects("/Users/ljystu/Library/CloudStorage/OneDrive-DelftUniversityofTechnology/thesis/pythonProject/repo_result_per_halfyear_100.json");
        for (Project p : projects) {
            String folderName = projectDownloader.downloadAndUnzip(p);
            uploadPackages(folderName);
        }


    }


}
