package ljystu.project.callgraph.config;

public class Path {
    static String mavenPath = "/opt/apache-maven-3.8.1";
    static String jarPath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";

    public Path() {
    }

    public static String getMavenPath() {
        return mavenPath;
    }

    public static void setMavenPath(String mavenPath) {
        Path.mavenPath = mavenPath;
    }

    public static String getJarPath() {
        return jarPath;
    }

    public static void setJarPath(String jarPath) {
        Path.jarPath = jarPath;
    }
}
