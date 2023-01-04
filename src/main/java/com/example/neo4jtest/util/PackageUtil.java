package com.example.neo4jtest.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PackageUtil {
    static String jarPath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";
    static String argLineLeft = "-noverify -javaagent:";
    static String getArgLineRight = "=incl=";

    public static List<Package> getCLasses() throws ClassNotFoundException {
        List<Package> definedPackages = new ArrayList<>();
        ClassUtil finder = new ClassUtil(new File("target/classes"));
        List<String> classFiles = finder.getClassFiles();
        for (String classFile : classFiles) {
            definedPackages.addAll(List.of(Class.forName(classFile).getClassLoader().getDefinedPackages()));
        }

        return definedPackages;
    }

    //    public static traverseDir()
    public static StringBuilder getPackages(HashSet<String> set, StringBuilder str, String jar) throws ClassNotFoundException {
        String argLine = argLineLeft + jar + getArgLineRight;

        List<Package> definedPackages = getCLasses();
        for (Package p : definedPackages) {
            String[] split = p.getName().split("\\.");
            if (split.length != 0) {
                str.append(split[0]);
                if (split.length > 1) str.append(".").append(split[1]);
            } else {
                str.append(p.getName());
            }
            str.append(".*");
            set.add(str.toString());
            str.setLength(0);
        }
        str.append(argLine);
        for (String s : set) {
            str.append(s).append(",");
        }
        str.setLength(str.length() - 1);
        str.append(";");
        return str;
    }
}
