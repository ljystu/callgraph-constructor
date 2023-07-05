
package ljystu.javacg.dyn;

import com.alibaba.fastjson.JSON;
import redis.clients.jedis.Jedis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;


public class MethodStack {
//    static Driver driver;

    private static Jedis jedis;
//    private static Neo4jUtil neo4jUtil;

//    private static Stack<Node> stack = new Stack<>();

    private static HashSet<Node> nodes = new HashSet<>();
    //    private static Map<Pair<String, String>, Integer> callgraph = new HashMap<>();
    private static HashSet<String> Edges = new HashSet<>();
    static FileWriter fw;

    //    static StringBuffer sb;
    static long threadid = -1L;

    //    public static Jedis jedis;
    public static final Object logFileLock = new Object();

    public static String redisKey;
    public static String packagePrefix;
    public static final ThreadLocal<Stack<Node>> stack = ThreadLocal.withInitial(Stack::new);

    public static Set<Edge> set = Collections.synchronizedSet(new HashSet<>());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {

                synchronized (logFileLock) {
//                jedis.close();

                    try {
                        System.out.println("write start");
                        String logPath =
//                            "/scratch/jingyuli/repos/analyzed_logs/";
                                "/Users/ljystu/Desktop/neo4j/call-graph-analysis/logs/";
                        String filePath = logPath + redisKey + ".log";
                        Path path = Paths.get(filePath);
                        if (!Files.exists(path)) {
                            Files.createFile(path);
                        }

                        BufferedWriter writer = new BufferedWriter(new FileWriter(
                                logPath + redisKey + ".log", true));

                        for (Edge edge : set) {
//                            System.out.println(JSON.toJSONString(edge));
                            writer.write(JSON.toJSONString(edge));
                            writer.newLine();
                        }
                        System.out.println("write over");
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

//        jedis = new Jedis("34.30.61.85");
//        jedis.auth("ljystu");


    }

    public static void push(String name) throws IOException {
        String[] strings = name.split(":");
        String packageName = strings[0];
        String className = strings[1];
        String methodName = strings[2];
        String params = strings[3];
        if (threadid == -1)
            threadid = Thread.currentThread().getId();

        if (Thread.currentThread().getId() != threadid)
            return;

        stack.get().push(getNodeFromName(packageName, className, methodName, params));
    }

    public static void pop(String info) throws IOException {
        if (threadid == -1)
            threadid = Thread.currentThread().getId();

        if (Thread.currentThread().getId() != threadid)
            return;

        Node calleeMethod = stack.get().pop();
        Node callerMethod = null;
        if (calleeMethod.getMethodName().equals("<init>")) {
            callerMethod = calleeMethod;
        } else {
            if (!stack.get().isEmpty()) {
                callerMethod = stack.get().peek();
            }
        }
        if (callerMethod == null) return;

        Edge edge = new Edge(callerMethod, calleeMethod);

        String[] strings = info.split("!");

        String packagePrefix = strings[0];
//        redisKey = strings[1];
        String artifactId = strings[1];

        set.add(edge);

//        setJsonString(edge, packagePrefix, artifactId);

    }

    private static Node getNodeFromName(String packageName, String className, String methodName, String params) {
        String[] split = params.split("&");
        return new Node(packageName, className, methodName, split[0], split[1]);
    }

    public static void setJsonString(Edge edge, String packagePrefix, String artifactId) {
//        if (edge.getFrom().getPackageName().startsWith(packagePrefix) || edge.getTo().getPackageName().startsWith(packagePrefix)) {
        jedis.sadd(artifactId, JSON.toJSONString(edge));
//        }
    }


}