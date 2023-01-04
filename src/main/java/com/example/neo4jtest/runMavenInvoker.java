package com.example.neo4jtest;

import com.example.neo4jtest.util.PackageUtil;
import org.apache.maven.shared.invoker.*;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

public class runMavenInvoker {
    public static void main(String[] args) throws Exception {
        String jarPath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";
        HashSet<String> set = new HashSet<>();

        // 获取Test类的所有import的类型
        StringBuilder str = new StringBuilder();

        PackageUtil.getPackages(set, str, jarPath);

        invoke(str);

        RedisOp.upload();
    }

    public static void invoke(StringBuilder str) throws Exception {
        Invoker invoker = new DefaultInvoker();

        // 设置Maven的安装目录
        invoker.setMavenHome(new File("/opt/apache-maven-3.8.1"));

        // 创建InvocationRequest
        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        request.setPomFile(new File("/Users/ljystu/Desktop/neo4j/pom.xml"));

        // 设置要执行的命令（这里是maven test）
        request.setGoals(Collections.singletonList("test"));

        // 设置参数
        Properties properties = new Properties();

        properties.setProperty("argLine", str.toString());
        request.setProperties(properties);

        // 执行maven命令
        InvocationResult result = invoker.execute(request);

        // 检查执行结果
        if (result.getExitCode() != 0) {
            throw new Exception("Maven命令执行失败");
        }
    }
}
