package ljystu.project.callgraph.config;

public class Constants {
    public static final String ARG_LINE_LEFT = "-noverify -javaagent:";
    public static final String ARG_LINE_RIGHT = "=incl=";

    public static final String DATABASE = "neo4j";

    //TODO paths should be parameters read into the main function
    public static String MAVEN_HOME =
//            "/scratch/jingyuli/apache-maven-3.8.1";
            "/opt/apache-maven-3.8.1";
    public static String JAVAAGENT_HOME =
//            "/scratch/jingyuli/repos/javacg-0.1-SNAPSHOT-dycg-agent.jar";
            "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";

    public static String JAVA_HOME =
//            "/apps/arch/2022r2/software/linux-rhel8-skylake_avx512/gcc-8.5.0/openjdk-11.0.12_7-k7npudpscbqwd5xat6m6xtdiqdjcn5ic";
            "/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home";

    public static String EXCLUSION_FILE =
//            "/scratch/jingyuli/exclusions.txt";
            "/Users/ljystu/Desktop/neo4j/call-graph-analysis/src/main/resources/exclusions.txt";

    public static final String NEO4J_PORT = "bolt://localhost:7687";
    public static final String NEO4J_USERNAME = "neo4j";
    public static final String NEO4J_PASSWORD = "ljystuneo";

    public static final String REDIS_ADDRESS = "34.175.14.212";
    public static final String PROJECT_LINK = "/archive/refs/heads/master.zip";

    public static final String PROJECT_LIST =
//            "/scratch/jingyuli/repos/project_files/org.apache.commons:commons-collections4:4.2.json";
            "/Users/ljystu/Desktop/pythonProject/commons_lang_project.json";
    public static String PROJECT_FOLDER =
//            "/scratch/jingyuli/repos/";
            "/Users/ljystu/Desktop/projects/";

    public static final int MONGO_PORT = 10899;
    public static final String username = "ljystu";

    public static final String password = "RmXrPQZ4GUsVpnDJUmg5";

    public static final String REDIS_PASSWORD = "ljystu";

    public static String PACKAGE_PREFIX;
}
