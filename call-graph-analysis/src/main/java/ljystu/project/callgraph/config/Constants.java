package ljystu.project.callgraph.config;

public class Constants {
    public static final String argLineLeft = "-noverify -javaagent:";
    public static final String argLineRight = "=incl=";

    public static final String DATABASE = "neo4j";

    public static final String MAVEN_HOME = "/opt/apache-maven-3.8.1";
    public static final String JAVAAGENT_HOME = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";

    public static final String JAVA_HOME = "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home";

    public static final String EXCLUSION_FILE = "src/main/resources/exclusions.txt";

    public static final String NEO4J_PORT = "bolt://localhost:7687";
    public static final String NEO4J_USERNAME = "neo4j";
    public static final String NEO4J_PASSWORD = "ljystuneo";

    public static final String REDIS_ADDRESS = "localhost";
    public static final String PROJECT_LINK = "/archive/refs/heads/master.zip";

    public static final String PROJECT_LIST = "/Users/ljystu/Desktop/pythonProject/repo_result_per_halfyear_100_test.json";

}
