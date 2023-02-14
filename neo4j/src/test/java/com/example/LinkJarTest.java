package com.example;

import ljystu.project.callgraph.util.PackageUtil;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;

public class LinkJarTest {
    @Test
    public void linkTest() {
        String jar = "hamcrest-core-1.3.jar";
//        Invoker invoker = new Invoker();
        String rootPath = "junit4-main";
        HashSet<String> dependencies = new HashSet<>();
        Map<String, String> coordinateMap = new PackageUtil().getJarToCoordMap(rootPath);
        System.out.println(coordinateMap.toString());
//        assertThat(coordinateMap.contains(jar),true);
        if (coordinateMap.containsKey(jar)) {
            System.out.println(coordinateMap.get(jar));
        }

    }
}
