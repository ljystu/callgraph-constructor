package com.example.neo4jtest.util;

import com.example.neo4jtest.RedisOp;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

public class invokerUtil {
    static Invoker invoker = new DefaultInvoker();

    public static void uploadPackages(String rootPath) throws Exception {
        String jarPath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";

        HashSet<String> set = new HashSet<>();


        // 获取Test类的所有import的类型
        StringBuilder str = new StringBuilder();

        PackageUtil.getPackages(set, str, jarPath, rootPath);

        invoke(str, rootPath, "test");

        RedisOp.upload();
    }

    public static void invoke(StringBuilder str, String rootPath, String task) throws Exception {


        // 设置Maven的安装目录
        invoker.setMavenHome(new File("/opt/apache-maven-3.8.1"));

        // 创建InvocationRequest
        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        request.setPomFile(new File(rootPath + "/pom.xml"));

        // 设置要执行的命令（这里是maven test）
        request.setGoals(Collections.singletonList(task));

        if (str.length() != 0) {
            Properties properties = new Properties();

            properties.setProperty("argLine", str.toString());
            request.setProperties(properties);

        }
        // 设置参数

        // 执行maven命令
        InvocationResult result = invoker.execute(request);

        // 检查执行结果
//        if (result.getExitCode() != 0) {
//            throw new Exception("Maven命令执行失败");
//        }
        File file = new File(rootPath).getAbsoluteFile();
        deleteFile(file);
    }

    public static boolean deleteFile(File dirFile) {
        // 如果dir对应的文件不存在，则退出
        if (!dirFile.exists()) {
            return false;
        }

        if (dirFile.isFile()) {
            return dirFile.delete();
        } else {

            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }

        return dirFile.delete();
    }

}
