package ljystu.project.callgraph.config;

public class Path {
    static String MAVEN_HOME = "/opt/apache-maven-3.8.1";
    static String JAVAAGENT_HOME = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";

    static String JAVA_HOME = "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home";

    public static String getJavaHome() {
        return JAVA_HOME;
    }

    public static void setJavaHome(String javaHome) {
        JAVA_HOME = javaHome;
    }

    public Path() {
    }

    public static String getMavenHome() {
        return MAVEN_HOME;
    }

    public static void setMavenHome(String mavenHome) {
        Path.MAVEN_HOME = mavenHome;
    }

    public static String getJavaagentHome() {
        return JAVAAGENT_HOME;
    }

    public static void setJavaagentHome(String javaagentHome) {
        Path.JAVAAGENT_HOME = javaagentHome;
    }
}
