package ljystu.project.callgraph.utils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        // Read the POM file with JSoup
        Document document;
        try {
            document = Jsoup.parse(new File(pomFile), StandardCharsets.UTF_8.name(), "", Parser.xmlParser());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("pomFile not found");
            return;
        }

        // Update the surefire-plugin and maven-compiler-plugin configurations
        updatePluginConfiguration(document, "maven-surefire-plugin", packageInfo);
//        updatePluginConfiguration(document, "maven-failsafe-plugin", packageInfo);


        // Write the modified POM file back to disk, preserving comments
        try {
            Files.write(Paths.get(pomFile), document.outerHtml().getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(pomFile + " update failed");
        }
    }

    private static void updatePluginConfiguration(Document document, String pluginName, String packageInfo) {
        // Find the plugin with the specified name
        Element plugin = null;
        try {
            plugin = document.select("plugin").stream()
                    .filter(p -> p.selectFirst("artifactId").text().equals(pluginName))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (plugin == null) {
            // Create a new plugin with the specified groupId and artifactId
            plugin = new Element("plugin");
            Element groupId = new Element("groupId");
            groupId.text("org.apache.maven.plugins");
            Element artifactId = new Element("artifactId");
            artifactId.text(pluginName);
            plugin.appendChild(groupId);
            plugin.appendChild(artifactId);

            // Append the new plugin to the build plugins list
            Element build = document.selectFirst("build");
            if (build == null) {
                build = new Element("build");
                document.selectFirst("project").appendChild(build);
            }
            Element plugins = build.selectFirst("plugins");
            if (plugins == null) {
                plugins = new Element("plugins");
                build.appendChild(plugins);
            }
            plugins.appendChild(plugin);
        }

        Element configuration = plugin.selectFirst("configuration");
        if (configuration == null) {
            configuration = new Element("configuration");
            plugin.appendChild(configuration);
        }

        if (pluginName.equals("maven-surefire-plugin")) {
            Element argLine = configuration.selectFirst("argLine");
            if (argLine == null) {
                argLine = new Element("argLine");
                configuration.appendChild(argLine);
            }
            if (argLine.text() != null && !argLine.text().contains(packageInfo)) {
                argLine.text(argLine.text() + " " + packageInfo);
            }
            Element parallel = configuration.selectFirst("parallel");
            if (parallel == null) {
                parallel = new Element("parallel");
                configuration.appendChild(parallel);
            }
            parallel.text("methods");

            // Set the threadCount property for parallelism
            Element threadCount = configuration.selectFirst("threadCount");
            if (threadCount == null) {
                threadCount = new Element("threadCount");
                configuration.appendChild(threadCount);
            }
            threadCount.text("10");
        } else {

            Element skipElement = configuration.selectFirst("skip");
            if (skipElement == null) {
                skipElement = new Element("skip");
                configuration.appendChild(skipElement);
            }
            skipElement.text("true");

        }

    }

    //    public static void editPOM(String pomFile, String packageInfo) {
//
//        MavenXpp3Reader reader = new MavenXpp3Reader();
//        Model model = null;
//        try {
//            model = reader.read(new FileReader(pomFile));
//        } catch (Exception e) {
//            log.error("pomFile not found");
//            return;
//        }
//
//        Build build = model.getBuild();
//        if (build == null) {
//            build = new Build();
//            model.setBuild(build);
//        }
//
//        // get plugins
//        List<Plugin> plugins = build.getPlugins();
////        PluginManagement pluginManagement = build.getPluginManagement();
////
////        if (pluginManagement == null) {
////            pluginManagement = new PluginManagement();
////        }
////        List<Plugin> plugins = pluginManagement.getPlugins();
//
//        Plugin surefirePlugin = new Plugin();
//        Plugin compilerPlugin = new Plugin();
//
//        boolean sureFireExists = false;
//        boolean compilerExists = false;
//
//        for (Plugin plugin : plugins) {
//            if (plugin.getArtifactId().equals("maven-surefire-plugin")) {
//                surefirePlugin = plugin;
//                sureFireExists = true;
//            }
//            if (plugin.getArtifactId().equals("maven-compiler-plugin")) {
//                compilerPlugin = plugin;
//                compilerExists = true;
//
//            }
//        }
//
//        //
//        updateSurefire("maven-surefire-plugin", packageInfo, plugins, surefirePlugin, sureFireExists);
////        updateSurefire("maven-compiler-plugin", packageInfo, plugins, compilerPlugin, compilerExists);
//        // write back to pom
//        MavenXpp3Writer writer = new MavenXpp3Writer();
//        try {
//            writer.write(new FileWriter(pomFile), model);
//        } catch (IOException e) {
//            e.printStackTrace();
//            log.error(pomFile + "update failed");
//        }
//    }
//
//    private static void updateSurefire(String name, String packageInfo, List<Plugin> plugins, Plugin plugin, boolean exists) {
//        if (!exists) {
//            plugin.setGroupId("org.apache.maven.plugins");
//            plugin.setArtifactId(name);
//        }
//
//        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
//        if (configuration == null) {
//            configuration = new Xpp3Dom("configuration");
//            plugin.setConfiguration(configuration);
//        }
//
//        if (name.equals("maven-surefire-plugin")) {
//
//            Xpp3Dom configArgLine = configuration.getChild("argLine");
//            if (configArgLine == null) {
//                configArgLine = new Xpp3Dom("argLine");
//                configuration.addChild(configArgLine);
//            }
//            if (configArgLine.getValue() != null) {
//                if (!configArgLine.getValue().contains(packageInfo)) {
//                    configArgLine.setValue(configArgLine.getValue() + " " + packageInfo);
//                }
//            } else {
//                configArgLine.setValue(packageInfo);
//            }
//        } else {
//            Xpp3Dom releaseArgLine = configuration.getChild("release");
//            if (releaseArgLine == null) {
//                releaseArgLine = new Xpp3Dom("release");
//                configuration.addChild(releaseArgLine);
//            }
//            releaseArgLine.setValue("11");
//
//            Xpp3Dom sourceArgLine = configuration.getChild("source");
//            if (sourceArgLine == null) {
//                sourceArgLine = new Xpp3Dom("source");
//                configuration.addChild(sourceArgLine);
//            }
//            sourceArgLine.setValue("1.6");
//
//            Xpp3Dom targetArgLine = configuration.getChild("target");
//            if (targetArgLine == null) {
//                targetArgLine = new Xpp3Dom("target");
//                configuration.addChild(targetArgLine);
//            }
//            targetArgLine.setValue("1.6");
//
//        }
//
//        if (!exists) {
//            plugins.add(plugin);
//        }
//    }
}
