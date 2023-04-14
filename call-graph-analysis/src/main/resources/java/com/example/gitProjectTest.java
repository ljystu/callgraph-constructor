package com.example;

import ljystu.project.callgraph.entity.Project;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class gitProjectTest {

    @Test
    public void gitDownloadTest(Project project) throws IOException, InterruptedException {


        String projectsDir = "/Users/ljystu/Desktop/projects/";
        try {
            String cloneCommand = "git clone " + project.getRepoUrl() + " " + project.getName();
            Process cloneProcess = Runtime.getRuntime().exec(cloneCommand, null, new File(projectsDir));
            cloneProcess.waitFor();
            System.out.println("Clone done");

            String path = projectsDir + project.getName();
            String cdCommand = "cd " + path;
            Process cdProcess = Runtime.getRuntime().exec(cdCommand);
            cdProcess.waitFor();

            String describeCommand = "git describe --tags --abbrev=0 HEAD~5 --always";
            Process describeProcess = Runtime.getRuntime().exec(describeCommand, null, new File(path));
            BufferedReader describeInput = new BufferedReader(new InputStreamReader(describeProcess.getInputStream()));
            String describeOutput = describeInput.readLine();
            describeInput.close();
            describeProcess.waitFor();
            System.out.println("Latest tag: " + describeOutput);


            String switchCommand = "git switch -c" + describeOutput;
            Process switchProcess = Runtime.getRuntime().exec(switchCommand, null, new File(path));
            switchProcess.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

