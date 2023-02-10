package ljystu.project.callgraph.util;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class POMUtil {
    public void editPOM(String pomFile, String packageInfo) throws Exception {
        // 读取 POM 文件

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pomFile));

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
            Xpp3Dom configArgLine = configuration.getChild("argLine");
            if (configArgLine == null) {
                configArgLine = new Xpp3Dom("argLine");
                configuration.addChild(configArgLine);
            }
            configArgLine.setValue(packageInfo);

            plugins.add(compilerPlugin);
        }

        build.setPluginManagement(pluginManagement);
        // 写入 POM 文件
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileWriter(pomFile), model);
    }
}
