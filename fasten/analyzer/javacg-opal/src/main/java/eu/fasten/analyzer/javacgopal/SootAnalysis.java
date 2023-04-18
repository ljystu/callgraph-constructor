package eu.fasten.analyzer.javacgopal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.fasten.analyzer.javacgopal.Util.GraphUtil;
import eu.fasten.analyzer.javacgopal.Util.MongodbUtil;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import redis.clients.jedis.Jedis;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author ljystu
 */
public class SootAnalysis {

    static Jedis jedis = new Jedis(Constants.MONGO_ADDRESS);

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
        String json = callGraphToJson(callGraph, prefix);

        try (FileWriter file = new FileWriter(outputPath)) {
            file.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        Map<String, String> redisMap = jedis.hgetAll("keys");
        for (Iterator<Edge> it = callGraph.iterator(); it.hasNext(); ) {
            Edge edge = it.next();
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();

            if (src.getDeclaringClass().getPackageName().startsWith(prefix) || tgt.getDeclaringClass().getPackageName().startsWith(prefix)) {
                StringBuilder srcParams = new StringBuilder();
                for (Type type : src.getParameterTypes()) {
                    String typeTransform = GraphUtil.typeTransform(type.toString());
                    srcParams.append(typeTransform).append(",");
                }

                if (srcParams.length() > 0) {
                    srcParams.deleteCharAt(srcParams.length() - 1);
                }

                StringBuilder tgtParams = new StringBuilder();
                for (Type type : src.getParameterTypes()) {
                    String typeTransform = GraphUtil.typeTransform(type.toString());
                    tgtParams.append(typeTransform).append(",");
                }
                if (tgtParams.length() > 0) {
                    tgtParams.deleteCharAt(tgtParams.length() - 1);
                }

                String srcTypeTransform = GraphUtil.typeTransform(tgt.getReturnType().toString());
                String tgtTypeTransform = GraphUtil.typeTransform(tgt.getReturnType().toString());

                String srcPackage = src.getDeclaringClass().getPackageName();

                if (!redisMap.containsKey(srcPackage)) {
                    redisMap.put(srcPackage, "not found");
                }
                GraphNode fromNode = new GraphNode(srcPackage, src.getDeclaringClass().getName(), src.getName(), tgtParams.toString(), srcTypeTransform,
                        redisMap.get(srcPackage));


                String tgtPackage = tgt.getDeclaringClass().getPackageName();

                if (!redisMap.containsKey(tgtPackage)) {
                    redisMap.put(tgtPackage, "not found");
                }

                GraphNode toNode = new GraphNode(tgtPackage, tgt.getDeclaringClass().getName(), tgt.getName(),
                        tgtParams.toString(), tgtTypeTransform, redisMap.get(tgtPackage));

                edges.add(new eu.fasten.analyzer.javacgopal.entity.Edge(fromNode, toNode));
            }
        }

        MongodbUtil.uploadEdges(edges, dependencyCoordinate);

    }
}