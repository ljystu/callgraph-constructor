package eu.fasten.analyzer.javacgopal.Util;

import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import eu.fasten.core.data.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIntPair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GraphUtil {
    public static HashMap<Integer, GraphNode> getNodes(PartialJavaCallGraph result) {
        HashMap<Integer, GraphNode> nodeHashMap = new HashMap<>();
        for (Map.Entry<JavaScope, Map<String, JavaType>> map : result.getClassHierarchy().entrySet()) {
            Map<String, JavaType> internalClasses = map.getValue();
            for (Map.Entry<String, JavaType> classes : internalClasses.entrySet()) {
                JavaType value = classes.getValue();
                Int2ObjectMap<JavaNode> methods = value.getMethods();

                for (Map.Entry<Integer, JavaNode> entry : methods.entrySet()) {
                    int key = entry.getKey();
                    if (nodeHashMap.containsKey(key)) continue;
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
                            str.append(param[i].substring(1)).append(",");
                        }
                        str.append(param[param.length - 1], 1, param[param.length - 1].length() - 1);
                    }


                    graphNode.setParams(str.toString());
                    graphNode.setReturnType(signature.substring(signature.indexOf(")") + 2));

                    nodeHashMap.put(key, graphNode);
                }

            }

        }
        return nodeHashMap;
    }

    public static HashSet<Edge> getAllEdges(PartialJavaCallGraph result, HashMap<Integer, GraphNode> nodes, String artifact) {
        HashSet<Edge> set = new HashSet<>();
        for (Map.Entry<IntIntPair, Map<Object, Object>> map : result.getGraph().getCallSites().entrySet()) {
            IntIntPair key = map.getKey();
            GraphNode nodeFrom = nodes.get(key.leftInt());
            nodeFrom.setCoordinate(artifact);
            GraphNode nodeTo = nodes.get(key.rightInt());
            nodeTo.setCoordinate(artifact);
            Edge edge = new Edge(nodeFrom, nodeTo);
            set.add(edge);
        }
        return set;
    }
}
