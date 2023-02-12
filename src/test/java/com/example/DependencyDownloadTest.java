package com.example;

import ljystu.project.callgraph.config.Path;
import ljystu.project.callgraph.util.Invoker;
import org.junit.Test;

import java.io.File;

public class DependencyDownloadTest {
    @Test
    public void invokeTest() {
        Invoker invoker = new Invoker();
        invoker.mavenInvoker.setMavenHome(new File(Path.getMavenHome()));
        invoker.invoke("/Users/ljystu/Desktop/neo4j/zookeeper-master", "dependency:copy-dependencies -DoutputDirecotry=/Users/ljystu/Desktop/neo4j/zookeeper-master/lib");
    }
}
