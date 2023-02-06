package ljystu.project.callgraph.config;

public class Arg {
    static String argLineLeft = "-noverify -javaagent:";
    static String getArgLineRight = "=incl=";

    public static String getArgLineLeft() {
        return argLineLeft;
    }

    public static String getGetArgLineRight() {
        return getArgLineRight;
    }

    public Arg() {
    }


}
