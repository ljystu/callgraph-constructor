package ljystu.project.callgraph.util;

import ljystu.project.callgraph.RedisOp;
import ljystu.project.callgraph.config.Paths;
import org.apache.maven.shared.invoker.*;
import org.apache.shiro.crypto.hash.Hash;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class Invoker {
    static org.apache.maven.shared.invoker.Invoker mavenInvoker = new DefaultInvoker();

    static String mavenPath = Paths.getMavenPath();
    static String jarPath = Paths.getJarPath();

    public HashSet<String> uploadPackages(String rootPath, HashMap<String, Integer> projectCount) throws Exception {

        HashSet<String> set = new HashSet<>();

        // 获取Test类的所有import的类型
        StringBuilder str = new StringBuilder();

        PackageUtil packageUtil = new PackageUtil();
        packageUtil.getPackages(projectCount, set, str, jarPath, rootPath);

        invoke(str, rootPath, "test");

        RedisOp redisOp = new RedisOp();
        redisOp.upload();

        return set;

    }

    public void invoke(StringBuilder str, String rootPath, String task) throws Exception {

        // 设置Maven的安装目录
        mavenInvoker.setMavenHome(new File(mavenPath));

        // 创建InvocationRequest
        InvocationRequest request = new DefaultInvocationRequest();

        // 设置项目的路径
        String projectMavenFilePath = rootPath + "/pom.xml";
        File projectMavenFile = new File(projectMavenFilePath);
        if (!projectMavenFile.exists()) {
            deleteFile(new File(rootPath).getAbsoluteFile());
            return;
        }
        request.setPomFile(projectMavenFile);

        // 设置要执行的命令（这里是maven test）
        request.setGoals(Collections.singletonList(task));

        if (str.length() != 0) {
            Properties properties = new Properties();

            properties.setProperty("argLine", str.toString());
            request.setProperties(properties);

        }
        // 执行maven命令
        mavenInvoker.execute(request);


        File projectDirectory = new File(rootPath).getAbsoluteFile();
        deleteFile(projectDirectory);
    }

    public void deleteFile(File dirFile) {
        // 如果dir对应的文件不存在，则退出
        if (!dirFile.exists()) {
            return;
        }

        if (dirFile.isFile()) {
            dirFile.delete();
            return;
        } else {

            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }

        dirFile.delete();
    }

}
