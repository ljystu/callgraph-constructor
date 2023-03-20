package ljystu.project.callgraph.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * The type Pom utils.
 */
@Slf4j
public class POMUtil {
    /**
     * edit surefire plugin, add javaagent to argline
     *
     * @param pomFile     the pom file
     * @param packageInfo the package info
     */
    public static void editPOM(String pomFile, String packageInfo) {

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            model = reader.read(new FileReader(pomFile));
        } catch (Exception e) {
            log.error("pomFile not found");
            return;
        }

        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        // get plugins
        List<Plugin> plugins = build.getPlugins();
//        PluginManagement pluginManagement = build.getPluginManagement();
//
//        if (pluginManagement == null) {
//            pluginManagement = new PluginManagement();
//        }
//        List<Plugin> plugins = pluginManagement.getPlugins();

        Plugin compilerPlugin = new Plugin();

        boolean exists = false;
        for (Plugin plugin : plugins) {
            if (plugin.getArtifactId().equals("maven-surefire-plugin")) {
                compilerPlugin = plugin;
                exists = true;
                break;
            }
        }

        //
        if (!exists) {
            compilerPlugin.setGroupId("org.apache.maven.plugins");
            compilerPlugin.setArtifactId("maven-surefire-plugin");
        }

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
        if (configArgLine.getValue() != null) {
            if (!configArgLine.getValue().contains(packageInfo)) {
                configArgLine.setValue(configArgLine.getValue() + " " + packageInfo);
            }
        } else {
            configArgLine.setValue(packageInfo);
        }

        if (!exists) {
            plugins.add(compilerPlugin);
        }

        // write back to pom
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try {
            writer.write(new FileWriter(pomFile), model);
        } catch (IOException e) {
            e.printStackTrace();
            log.error(pomFile + "update failed");
        }
    }
}
