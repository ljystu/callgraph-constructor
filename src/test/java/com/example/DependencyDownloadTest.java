package com.example;

import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.invoker.Invoker;
import org.junit.Test;

import java.io.File;

public class DependencyDownloadTest {
    @Test
    public void invokeJarDownloadTest() {
        Invoker invoker = new Invoker("junit4-main");
        invoker.mavenInvoker.setMavenHome(new File(Constants.MAVEN_HOME));
        invoker.invokeTask("dependency:copy-dependencies", "./lib");
    }
}
