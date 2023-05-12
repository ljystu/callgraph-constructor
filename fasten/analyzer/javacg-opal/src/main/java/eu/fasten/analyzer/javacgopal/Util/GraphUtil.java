package eu.fasten.analyzer.javacgopal.Util;

import eu.fasten.analyzer.javacgopal.Constants;
import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import eu.fasten.core.data.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

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

                    String substring = signature.substring(signature.indexOf("(") + 1, signature.indexOf(")"));
                    String[] param = substring.split(",");
                    StringBuilder str = new StringBuilder();
                    if (param.length > 0) {
                        for (int i = 0; i < param.length; i++) {
                            if (param[i].length() == 0) break;
                            String paramType = param[i].substring(1);
                            paramType = typeTransform(paramType);
                            str.append(paramType).append(",");
                        }
                        if (str.length() > 0)
                            str.setLength(str.length() - 1);
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
        if (type.startsWith("[")) {
            int arrayDimension = 0;
            while (type.startsWith("[")) {
                arrayDimension++;
                type = type.substring(1);
            }

            String fullQualifiedType;
            switch (type) {
                case "B":
                    fullQualifiedType = "byte";
                    break;
                case "C":
                    fullQualifiedType = "char";
                    break;
                case "D":
                    fullQualifiedType = "double";
                    break;
                case "F":
                    fullQualifiedType = "float";
                    break;
                case "I":
                    fullQualifiedType = "int";
                    break;
                case "J":
                    fullQualifiedType = "long";
                    break;
                case "S":
                    fullQualifiedType = "short";
                    break;
                case "Z":
                    fullQualifiedType = "boolean";
                    break;
                default:
                    fullQualifiedType = type.substring(1, type.length() - 1); // Remove 'L' and ';'
            }

            for (int i = 0; i < arrayDimension; i++) {
                fullQualifiedType += "[]";
            }
            return fullQualifiedType;
        }

        switch (type) {
            case "java.lang.BooleanType":
                return "boolean";
            case "java.lang.VoidType":
                return "void";
            case "java.lang.IntegerType":
                return "int";
            case "java.lang.LongType":
                return "long";
            case "java.lang.FloatType":
                return "float";
            case "java.lang.DoubleType":
                return "double";
            case "java.lang.ByteType":
                return "byte";
            case "java.lang.ShortType":
                return "short";
            case "java.lang.CharacterType":
            case "java.lang.CharType":
                return "char";
            case "java.lang.BooleanType[]":
                return "boolean[]";
            case "java.lang.VoidType[]":
                return "void[]";
            case "java.lang.IntegerType[]":
                return "int[]";
            case "java.lang.LongType[]":
                return "long[]";
            case "java.lang.FloatType[]":
                return "float[]";
            case "java.lang.DoubleType[]":
                return "double[]";
            case "java.lang.ByteType[]":
                return "byte[]";
            case "java.lang.ShortType[]":
                return "short[]";
            case "java.lang.CharacterType[]":
            case "java.lang.CharType[]":
                return "char[]";
            default:
                return type;
        }
    }

    public static HashSet<Edge> getAllEdges(PartialJavaCallGraph result, HashMap<Integer, GraphNode> nodes, String artifact, String version) {
        HashSet<Edge> set = new HashSet<>();
        jedis.auth("ljystu");
        String dependencyWithoutVersion = artifact.substring(0, artifact.lastIndexOf(":"));
        String dependencyWithVersion = dependencyWithoutVersion + ":" + version;
        Map<String, String> redisMap =
//                readFromFile();
                jedis.hgetAll(dependencyWithVersion);
        for (Map.Entry<String, String> map : redisMap.entrySet()) {
            if (map.getValue().startsWith(dependencyWithoutVersion)) {
                map.setValue(dependencyWithVersion);
            }
        }
        for (Map.Entry<IntIntPair, Map<Object, Object>> map : result.getGraph().getCallSites().entrySet()) {
            IntIntPair key = map.getKey();

            GraphNode nodeFrom = nodes.get(key.leftInt());

            GraphNode nodeTo = nodes.get(key.rightInt());

            Pattern pattern = Pattern.compile("^(java|javax|jdk|sun|com\\.sun|org\\.w3c|org\\.xml|org\\.ietf|org\\.omg|org\\.jcp).*");
            if (pattern.matcher(nodeFrom.getPackageName()).matches() || pattern.matcher(nodeTo.getPackageName()).matches()) {
                continue;
            }

            if (!redisMap.containsKey(nodeFrom.getPackageName())) {
                redisMap.put(nodeFrom.getPackageName(), "not found");
            }
            nodeFrom.setCoordinate(redisMap.get(nodeFrom.getPackageName()));


            if (!redisMap.containsKey(nodeTo.getPackageName())) {
                redisMap.put(nodeTo.getPackageName(), "not found");
            }

            nodeTo.setCoordinate(redisMap.get(nodeTo.getPackageName()));

            Edge edge = new Edge(nodeFrom, nodeTo);
            set.add(edge);
        }
        jedis.close();
        return set;
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
