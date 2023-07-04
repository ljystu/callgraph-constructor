package eu.fasten.analyzer.javacgopal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.fasten.analyzer.javacgopal.Util.GraphUtil;
import eu.fasten.analyzer.javacgopal.Util.MongodbUtil;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author ljystu
 */
public class SootAnalysis {

    static Jedis jedis = new Jedis(Constants.MONGO_ADDRESS, 6379, 30000);

    public static void main(String[] args) {
        jedis.auth("ljystu");

        String prefix = args[0];
        String dependencyCoordinate = args[1];
        String jarPath = args[2];

        System.out.println("analyzing " + dependencyCoordinate + " ..." + args[2]);
        String outputPath = "/Users/ljystu/Desktop/projects/soot-" + dependencyCoordinate + ".json";
        setupSoot(jarPath);

        // Run a Soot analysis, such as the Call Graph construction
        PackManager.v().runPacks();

        // Get the Call Graph
        CallGraph callGraph = Scene.v().getCallGraph();

        // Convert the Call Graph to JSON
//        String json = callGraphToJson(callGraph, prefix);
//
//        try (FileWriter file = new FileWriter(outputPath)) {
//            file.write(json);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        callGraphToMongo(callGraph, prefix, dependencyCoordinate);

        // Write the JSON to a file

        jedis.close();
    }

    private static void setupSoot(String jarPath) {
        String jrtFsJarPath = "/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home/lib/jrt-fs.jar";
        String sootClassPath;

        sootClassPath = jarPath + File.pathSeparator + jrtFsJarPath;

        // Set Soot options
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_soot_classpath(sootClassPath);
        Options.v().set_output_format(Options.output_format_none);

        if (new File(jrtFsJarPath).exists()) {
            Options.v().set_prepend_classpath(true);
            Options.v().set_src_prec(Options.src_prec_only_class);
//            Scene.v().setJava9Mode(true);
        }

        // Load the classes from the JAR file
        Scene.v().loadNecessaryClasses();
    }

    private static String callGraphToJson(CallGraph callGraph, String prefix) {
        List<Map<String, String>> edges = new ArrayList<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (Iterator<Edge> it = callGraph.iterator(); it.hasNext(); ) {
            Edge edge = it.next();
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();

            Map<String, String> edgeInfo = new HashMap<>();
            if (src.getDeclaringClass().getPackageName().startsWith(prefix) && tgt.getDeclaringClass().getPackageName().startsWith(prefix)) {

                edgeInfo.put("srcPackage", src.getDeclaringClass().getPackageName());
                edgeInfo.put("srcClass", src.getDeclaringClass().getName());
                edgeInfo.put("srcMethod", src.getName());
                edgeInfo.put("tgtPackage", tgt.getDeclaringClass().getPackageName());
                edgeInfo.put("tgtClass", tgt.getDeclaringClass().getName());
                edgeInfo.put("tgtMethod", tgt.getName());
                edges.add(edgeInfo);
            }
        }

        return gson.toJson(edges);
    }

    private static void callGraphToMongo(CallGraph callGraph, String prefix, String dependencyCoordinate) {
        HashSet<eu.fasten.analyzer.javacgopal.entity.Edge> edges = new HashSet<>();

        String dependencyWithoutVersion = dependencyCoordinate.substring(0, dependencyCoordinate.lastIndexOf(":"));

        Map<String, String> packageToCoordinateMap =
//                readFromFile();
//                new HashMap<>();
////                getRedisMap();
                jedis.hgetAll(dependencyCoordinate);
        for (Map.Entry<String, String> map : packageToCoordinateMap.entrySet()) {
            if (map.getValue().startsWith(dependencyWithoutVersion)) {
                map.setValue(dependencyCoordinate);
            }
        }
        for (Iterator<Edge> it = callGraph.iterator(); it.hasNext(); ) {
            Edge edge = it.next();
            SootMethod srcMethod = edge.src();
            SootMethod tgtMethod = edge.tgt();

            Pattern pattern = Pattern.compile("^(java|javax|jdk|sun|com\\.sun|org\\.w3c|org\\.xml|org\\.ietf|org\\.omg|org\\.jcp).*");
            if (pattern.matcher(srcMethod.getDeclaringClass().getPackageName()).matches() || pattern.matcher(tgtMethod.getDeclaringClass().getPackageName()).matches()) {
                continue;
            }
            if (srcMethod.getDeclaringClass().getPackageName().startsWith(prefix) || tgtMethod.getDeclaringClass().getPackageName().startsWith(prefix)) {
                edges.add(new eu.fasten.analyzer.javacgopal.entity.Edge(buildNode(packageToCoordinateMap, srcMethod), buildNode(packageToCoordinateMap, tgtMethod)));
            }
        }

        MongodbUtil.uploadEdges(edges, dependencyCoordinate);

    }

    private static GraphNode buildNode(Map<String, String> redisMap, SootMethod method) {
        //params
        StringBuilder tgtParams = new StringBuilder();
        for (Type type : method.getParameterTypes()) {
            String typeTransform = GraphUtil.typeTransform(type.toString());
            tgtParams.append(typeTransform).append(",");
        }
        if (tgtParams.length() > 0) {
            tgtParams.deleteCharAt(tgtParams.length() - 1);
        }
        int modifiers = method.getModifiers();
        String access = "private";
        switch (modifiers) {
            case 1:
                access = "public";
                break;
            case 2:
                access = "private";
                break;
            case 4:
                access = "protected";
                break;
            default:
                access = "private";
                break;
        }
//        if(Modifier.isPublic(modifiers)) {
//            System.out.println("Method is public");
//        }
//
//        if(Modifier.isPrivate(modifiers)) {
//            System.out.println("Method is private");
//        }
//
//        if(Modifier.isProtected(modifiers)) {
//            System.out.println("Method is protected");
//        }
        if (method.getName().equals("<clinit>") || method.getName().equals("<init>")) {
            access = "public";
        }


        String tgtTypeTransform = GraphUtil.typeTransform(method.getReturnType().toString());

        String tgtPackage = method.getDeclaringClass().getPackageName();

        //coordinate
        if (!redisMap.containsKey(tgtPackage)) {
            redisMap.put(tgtPackage, "not found");
        }
        String tgtClassName = method.getDeclaringClass().getName();
        if (tgtClassName.contains(".")) {
            tgtClassName = tgtClassName.substring(tgtClassName.lastIndexOf(".") + 1);
        }
        return new GraphNode(tgtPackage, tgtClassName, method.getName(),
                tgtParams.toString(), tgtTypeTransform, redisMap.get(tgtPackage), access);
    }

    public static Map<String, String> getRedisMap() {
        Map<String, String> map = new HashMap<>();
        ScanParams params = new ScanParams().match("*");
        String cursor = "0";
        do {
            ScanResult<Map.Entry<String, String>> scanResult = jedis.hscan("keys", cursor, params);
            List<Map.Entry<String, String>> entries = scanResult.getResult();
            for (Map.Entry<String, String> entry : entries) {
                map.put(entry.getKey(), entry.getValue());
            }
            cursor = scanResult.getStringCursor();
        } while (!"0".equals(cursor));
        return map;
    }

    public static Map<String, String> readFromFile() {
        Map<String, String> map = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/Users/ljystu/Desktop/projects/redisKeys.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(" ");
                map.put(split[0], split[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}