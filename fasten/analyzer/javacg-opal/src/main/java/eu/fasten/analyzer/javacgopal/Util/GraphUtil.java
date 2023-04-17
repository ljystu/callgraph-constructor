package eu.fasten.analyzer.javacgopal.Util;

import eu.fasten.analyzer.javacgopal.Constants;
import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import eu.fasten.core.data.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphUtil {
    static Jedis jedis = new Jedis(Constants.MONGO_ADDRESS);

    public static HashMap<Integer, GraphNode> getNodes(PartialJavaCallGraph result) {
        HashMap<Integer, GraphNode> nodeHashMap = new HashMap<>();
        for (Map.Entry<JavaScope, Map<String, JavaType>> map : result.getClassHierarchy().entrySet()) {
            Map<String, JavaType> internalClasses = map.getValue();
            for (Map.Entry<String, JavaType> classes : internalClasses.entrySet()) {
                JavaType value = classes.getValue();
                Int2ObjectMap<JavaNode> methods = value.getMethods();

                for (Map.Entry<Integer, JavaNode> entry : methods.entrySet()) {
                    int key = entry.getKey();
                    if (nodeHashMap.containsKey(key)) {
                        continue;
                    }
                    FastenURI uri = entry.getValue().getUri();
                    String rawEntity = uri.getRawEntity();
                    GraphNode graphNode = new GraphNode();

                    graphNode.setPackageName(uri.getNamespace());
                    graphNode.setClassName(rawEntity.substring(0, rawEntity.indexOf(".")));

                    String signature = entry.getValue().getSignature();
                    graphNode.setMethodName(signature.substring(0, signature.indexOf("(")));
                    signature = signature.replace("/", ".");

                    String substring = signature.substring(signature.indexOf("("), signature.indexOf(")") + 1);
                    String[] param = substring.split(",");
                    StringBuilder str = new StringBuilder();
                    if (param.length != 1) {
                        for (int i = 1; i < param.length - 1; i++) {
                            String paramType = param[i].substring(1);
                            paramType = typeTransform(paramType);
                            str.append(paramType).append(",");
                        }
                        str.append(param[param.length - 1], 1, param[param.length - 1].length() - 1);
                    }


                    graphNode.setParams(str.toString());
                    String returnType = signature.substring(signature.indexOf(")") + 2);
                    returnType = typeTransform(returnType);
                    graphNode.setReturnType(returnType);

                    nodeHashMap.put(key, graphNode);
                }

            }

        }
        return nodeHashMap;
    }

    public static String typeTransform(String type) {
        if (type.equals("java.lang.BooleanType[]")) {
            type = "boolean[]";
        }
        if (type.equals("java.lang.IntegerType[]")) {
            type = "int[]";
        }
        if (type.equals("java.lang.LongType[]")) {
            type = "long[]";
        }
        if (type.equals("java.lang.FloatType[]")) {
            type = "float[]";
        }
        if (type.equals("java.lang.DoubleType[]")) {
            type = "double[]";
        }
        if (type.equals("java.lang.ByteType[]")) {
            type = "byte[]";
        }
        if (type.equals("java.lang.ShortType[]")) {
            type = "short[]";
        }
        if (type.equals("java.lang.CharacterType[]")) {
            type = "char[]";
        }
        if (type.equals("java.lang.BooleanType")) {
            type = "boolean";
        }
        if (type.equals("java.lang.VoidType")) {
            type = "void";
        }
        if ("java.lang.IntegerType".equals(type)) {
            type = "int";
        }
        if (type.equals("java.lang.LongType")) {
            type = "long";
        }
        if (type.equals("java.lang.FloatType")) {
            type = "float";
        }
        if (type.equals("java.lang.DoubleType")) {
            type = "double";
        }
        if (type.equals("java.lang.ByteType")) {
            type = "byte";
        }
        if (type.equals("java.lang.ShortType")) {
            type = "short";
        }
        if (type.equals("java.lang.CharacterType")) {
            type = "char";
        }
        return type;
    }

    public static HashSet<Edge> getAllEdges(PartialJavaCallGraph result, HashMap<Integer, GraphNode> nodes, String artifact) {
        HashSet<Edge> set = new HashSet<>();
        jedis.auth("ljystu");
        Set<String> keys = jedis.keys("*");
        HashMap<String, String> redisMap = new HashMap<>();
        for (Map.Entry<IntIntPair, Map<Object, Object>> map : result.getGraph().getCallSites().entrySet()) {
            IntIntPair key = map.getKey();

            GraphNode nodeFrom = nodes.get(key.leftInt());

            if (keys.contains(nodeFrom.getPackageName()) && !redisMap.containsKey(nodeFrom.getPackageName())) {
                redisMap.put(nodeFrom.getPackageName(), jedis.get(nodeFrom.getPackageName()));
            } else {
                redisMap.put(nodeFrom.getPackageName(), "not found");
            }
            nodeFrom.setCoordinate(redisMap.get(nodeFrom.getPackageName()));

            GraphNode nodeTo = nodes.get(key.rightInt());
            if (keys.contains(nodeTo.getPackageName()) && !redisMap.containsKey(nodeTo.getPackageName())) {
                redisMap.put(nodeTo.getPackageName(), jedis.get(nodeTo.getPackageName()));
            } else {
                redisMap.put(nodeTo.getPackageName(), "not found");
            }

            nodeTo.setCoordinate(redisMap.get(nodeTo.getPackageName()));

            Edge edge = new Edge(nodeFrom, nodeTo);
            set.add(edge);
        }
        return set;
    }
}
