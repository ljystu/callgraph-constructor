package ljystu.project.callgraph.config;

public class Constants {
    public static final String ARG_LINE_LEFT = "-noverify -javaagent:";
    public static final String ARG_LINE_RIGHT = "=incl=";

    public static final String DATABASE = "neo4j";

    //TODO paths should be parameters read into the main function
    public static String MAVEN_HOME;
    public static String JAVAAGENT_HOME;

    public static String JAVA_HOME;

    public static String EXCLUSION_FILE = "/Users/ljystu/Desktop/neo4j/call-graph-analysis/src/main/resources/exclusions.txt";
    public static final String NEO4J_PORT = "bolt://localhost:7687";
    public static final String NEO4J_USERNAME = "neo4j";
    public static final String NEO4J_PASSWORD = "ljystuneo";

    public static final String SERVER_IP_ADDRESS = "34.175.14.212";
    public static final String PROJECT_LINK = "/archive/refs/heads/master.zip";

    public static String PROJECT_FOLDER;


    public static final int MONGO_PORT = 10899;
    public static final String USERNAME = "ljystu";

    public static final String MONGO_PASSWORD = "RmXrPQZ4GUsVpnDJUmg5";

    public static final String REDIS_PASSWORD = "ljystu";

    public static String PACKAGE_PREFIX;
}
