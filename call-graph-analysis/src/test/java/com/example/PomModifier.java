package com.example;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.FileReader;
import java.io.FileWriter;

public class PomModifier {

    public static void main(String[] args) throws Exception {
        // 读取pom.xml
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("src/main/resources/pom.xml"));

        // 获取build节点
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        // 获取编译插件
        Plugin compilerPlugin = build.getPluginsAsMap().get("org.apache.maven.plugins:maven-surefire-plugin");
        if (compilerPlugin == null) {
            compilerPlugin = new Plugin();
            compilerPlugin.setGroupId("org.apache.maven.plugins");
            compilerPlugin.setArtifactId("maven-surefire-plugin");
            build.addPlugin(compilerPlugin);
        }

        // 修改插件配置
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

        // 写回pom.xml
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter("src/main/resources/pom.xml"), model);
    }
}
