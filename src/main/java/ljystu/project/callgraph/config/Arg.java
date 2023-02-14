package ljystu.project.callgraph.config;

public class Arg {
    static String argLineLeft = "-noverify -javaagent:";
    static String getArgLineRight = "=incl=";

    static String DATABASE = "neo4j";

    public static String getDATABASE() {
        return DATABASE;
    }

    public static String getArgLineLeft() {
        return argLineLeft;
    }

    public static String getGetArgLineRight() {
        return getArgLineRight;
    }

    public Arg() {
    }


}
