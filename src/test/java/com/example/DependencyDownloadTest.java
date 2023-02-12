package com.example;

import ljystu.project.callgraph.config.Path;
import ljystu.project.callgraph.util.Invoker;
import org.junit.Test;

import java.io.File;

public class DependencyDownloadTest {
    @Test
    public void invokeJarDownloadTest() {
        Invoker invoker = new Invoker();
        invoker.mavenInvoker.setMavenHome(new File(Path.getMavenHome()));
        invoker.invokeTask("junit4-main", "dependency:copy-dependencies", "./lib");
    }
}
