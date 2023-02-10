package com.example;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

public class POMEditor {
    public static void main(String[] args) throws Exception {
        // 读取 POM 文件
        File pomFile = new File("src/main/resources/pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("src/main/resources/pom.xml"));

        // 获取构建部分
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        // 获取插件列表
//        List<Plugin> plugins = build.getPlugins();
        PluginManagement pluginManagement = build.getPluginManagement();

        if (pluginManagement == null) {
            pluginManagement = new PluginManagement();
        }
        List<Plugin> plugins = pluginManagement.getPlugins();
        // 判断插件是否已存在
        boolean exists = false;
        for (Plugin plugin : plugins) {
            if (plugin.getArtifactId().equals("maven-surefire-plugin")) {
                exists = true;
                break;
            }
        }

        // 如果不存在则添加插件
        if (!exists) {
            Plugin compilerPlugin = new Plugin();
            compilerPlugin.setGroupId("org.apache.maven.plugins");
            compilerPlugin.setArtifactId("maven-surefire-plugin");
//            compilerPlugin.setVersion("3.8.0");
            Xpp3Dom configuration = (Xpp3Dom) compilerPlugin.getConfiguration();
            if (configuration == null) {
                configuration = new Xpp3Dom("configuration");
                compilerPlugin.setConfiguration(configuration);
            }
            Xpp3Dom source = configuration.getChild("argLine");
            if (source == null) {
                source = new Xpp3Dom("argLine");
                configuration.addChild(source);
            }
            source.setValue("-noverify -javaagent:/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar=incl=org.junit.*,org.apache.*,org.slf4j.*;");
//            compilerPlugin.setConfiguration(configuration);
            plugins.add(compilerPlugin);
        }
        build.setPluginManagement(pluginManagement);

        // 写入 POM 文件
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter(pomFile), model);
    }
}
