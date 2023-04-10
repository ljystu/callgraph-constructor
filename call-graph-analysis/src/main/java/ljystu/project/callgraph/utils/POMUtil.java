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
        updatePluginConfiguration(document, "maven-failsafe-plugin", packageInfo);


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
            plugin = document.selectFirst("build > plugins > plugin > artifactId:contains(" + pluginName + ") ~ configuration");
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
            plugin.appendChild(new Element("configuration"));

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

        editPluginConfiguration(pluginName, packageInfo, configuration);

    }

    private static void editPluginConfiguration(String pluginName, String packageInfo, Element configuration) {
        if ("maven-surefire-plugin".equals(pluginName)) {
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

//             Set the threadCount property for parallelism
            Element useUnlimitedThreads = configuration.selectFirst("useUnlimitedThreads");
            if (useUnlimitedThreads == null) {
                useUnlimitedThreads = new Element("useUnlimitedThreads");
                configuration.appendChild(useUnlimitedThreads);
            }
            useUnlimitedThreads.text("true");


            //timeout in seconds
            Element timeoutInSeconds = configuration.selectFirst("parallelTestsTimeoutInSeconds");
            if (timeoutInSeconds == null) {
                timeoutInSeconds = new Element("parallelTestsTimeoutInSeconds");
                configuration.appendChild(timeoutInSeconds);
            }
            timeoutInSeconds.text("600");

//            Element timeoutForcedInSeconds = configuration.selectFirst("parallelTestsTimeoutForcedInSeconds");
//            if (timeoutForcedInSeconds == null) {
//                timeoutForcedInSeconds = new Element("parallelTestsTimeoutForcedInSeconds");
//                configuration.appendChild(timeoutForcedInSeconds);
//            }
//            timeoutForcedInSeconds.text("1800");

            Element timeoutForcedInSeconds = configuration.selectFirst("forkedProcessTimeoutInSeconds");
            if (timeoutForcedInSeconds == null) {
                timeoutForcedInSeconds = new Element("forkedProcessTimeoutInSeconds");
                configuration.appendChild(timeoutForcedInSeconds);
            }
            timeoutForcedInSeconds.text("600");

            Element reuseForks = configuration.selectFirst("reuseForks");
            if (reuseForks == null) {
                reuseForks = new Element("reuseForks");
                configuration.appendChild(reuseForks);
            }
            reuseForks.text("false");
//            Element forkCounts = configuration.selectFirst("forkCount");
//            if (forkCounts == null) {
//                forkCounts = new Element("forkCount");
//                configuration.appendChild(forkCounts);
//            }
//            forkCounts.text("1");
//
            Element testFailureIgnore = configuration.selectFirst("testFailureIgnore");
            if (testFailureIgnore == null) {
                testFailureIgnore = new Element("testFailureIgnore");
                configuration.appendChild(testFailureIgnore);
            }
            testFailureIgnore.text("true");


        } else {
            Element skipElement = configuration.selectFirst("skip");
            if (skipElement == null) {
                skipElement = new Element("skip");
                configuration.appendChild(skipElement);
            }
            skipElement.text("true");

        }
    }


}
