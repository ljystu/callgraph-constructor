package ljystu.project.callgraph.util;


import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;

import java.util.*;
import java.util.List;

import static org.neo4j.driver.Values.parameters;


public class Neo4jUtil {

    // Driver objects are thread-safe and are typically made available application-wide.
    Driver driver;

    String DATABASE = "neo4j";

    public Neo4jUtil() {

    }

    public void init(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

    }

    public void addEdge(Edge edge) {
        try (Session session = driver.session(SessionConfig.forDatabase(DATABASE))) {
            Node from = edge.getFrom();
            Node to = edge.getTo();
            session.executeWrite(tx -> tx.run("MATCH (method_from:Method {packageName: $packagename, className: $classname, " +
                            "methodName: $methodname, params: $params, returnType: $returntype }), " +
                            "(method_to:Method {packageName: $packagename2, className: $classname2, " +
                            "methodName: $methodname2, params: $params2, returnType: $returntype2}) " +
                            "MERGE (method_from)-[r:CALLS]->(method_to)",
                    parameters("packagename", from.getPackageName(),
                            "classname", from.getClassName(), "methodname", from.getMethodName(),
                            "params", from.getParams(), "returntype", from.getReturnType(), "packagename2", to.getPackageName(),
                            "classname2", to.getClassName(), "methodname2", to.getMethodName(),
                            "params2", to.getParams(), "returntype2", to.getReturnType())));
//            System.out.println(result.next().toString());
        }
    }

    public void addEdgeUnwind(List<HashMap<String, Object>> fromNodes, List<HashMap<String, Object>> toNodes, String label) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fromNodes", fromNodes);
        parameters.put("label", label);

        try (Session session = driver.session(SessionConfig.forDatabase(DATABASE))) {

            session.executeWrite(tx -> tx.run("UNWIND $fromNodes as row " +
                            "MATCH (method_from:Method {packageName: row.packageName, className: row.className, " +
                            "methodName: row.methodName, params: row.params, returnType: row.returnType }), " +
                            "(method_to:Method {packageName: row.packageName2, className: row.className2, " +
                            "methodName: row.methodName2, params: row.params2, returnType: row.returnType2}) " +
                            "MERGE (method_from)-[r:CALL]->(method_to)" +
                            "ON CREATE SET r.label = $label \n" +
                            "ON MATCH SET r.label = 'both'",
                    parameters));
//            System.out.println(result.next().toString());
        }
    }

    public void addMethod(Node node) {
        String packageName = node.getPackageName();
        String className = node.getClassName();
        String methodName = node.getMethodName();
        String params = node.getParams();
        String returnType = node.getReturnType();
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session(SessionConfig.forDatabase(DATABASE))) {
            // Wrapping a Cypher Query in a Managed Transaction provides atomicity
            // and makes handling errors much easier.
            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
            // These methods are also able to handle connection problems and transient errors using an automatic retry mechanism.
            session.executeWrite(tx -> tx.run("MERGE (a:Method {packageName: $packagename, className: $classname," +
                            " methodName: $methodname, params: $params, returnType: $returntype })",
                    parameters("packagename", packageName, "classname", className,
                            "methodname", methodName, "params", params, "returntype", returnType)));
        }
    }

    public void addMethodUnwind(List<HashMap<String, Object>> list) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batches", list);
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session(SessionConfig.forDatabase(DATABASE))) {
            // Wrapping a Cypher Query in a Managed Transaction provides atomicity
            // and makes handling errors much easier.
            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
            // These methods are also able to handle connection problems and transient errors using an automatic retry mechanism.
            session.executeWrite(tx -> tx.run("UNWIND $batches as row " +
                            "MERGE (a:Method {packageName: row.packageName, className: row.className," +
                            " methodName: row.methodName, params: row.params, returnType: row.returnType })",
                    parameters));
        }
    }

    public void printMethods(String initial) {
        try (Session session = driver.session(SessionConfig.forDatabase(DATABASE))) {
            // A Managed transaction is a quick and easy way to wrap a Cypher Query.
            // The `session.run` method will run the specified Query.
            // This simpler method does not use any automatic retry mechanism.
            Result result = session.run(
                    "MATCH (a:Method) WHERE a.name STARTS WITH $x RETURN a.name AS name",
                    parameters("x", initial));
            // Each Cypher execution returns a stream of records.
            while (result.hasNext()) {
                Record records = result.next();
                // Values can be extracted from a record by index or name.
                System.out.println(records.get("name").asString());
            }
        }
    }

    public void close() {
        // Closing a driver immediately shuts down all open connections.
        driver.close();
    }

    public void upload(HashSet<Edge> edges, HashSet<Node> Nodes) {
        init("bolt://localhost:7687", "neo4j", "ljystu");

        for (Node node : Nodes) {
            addMethod(node);
        }
        for (Edge edge : edges) {
            addEdge(edge);
        }
//        printMethods("c");
        close();
    }

    public void upload(Edge edge) {
//        init("bolt://localhost:7687", "neo4j", "ljystu");
        addMethod(edge.getFrom());
        addMethod(edge.getTo());
        addEdge(edge);
//        close();
    }

    public void uploadBatch(List<Node> nodesList, List<Edge> edges, String label) {
        List<HashMap<String, Object>> nodeMap = new ArrayList<>();
        for (Node node : nodesList) {
            nodeMap.add(getNodeInfo(node, ""));
        }
        addMethodUnwind(nodeMap);
        List<HashMap<String, Object>> fromNodes = new ArrayList<>();
        List<HashMap<String, Object>> toNodes = new ArrayList<>();
        for (Edge edge : edges) {
            Node from = edge.getFrom();
            Node to = edge.getTo();

            HashMap<String, Object> nodeInfo = getNodeInfo(from, "");
            nodeInfo.putAll(getNodeInfo(to, "2"));
            fromNodes.add(nodeInfo);
        }
        addEdgeUnwind(fromNodes, toNodes, label);
    }

    public HashMap<String, Object> getNodeInfo(Node node, String info) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("packageName" + info, node.getPackageName());
        map.put("className" + info, node.getClassName());
        map.put("methodName" + info, node.getMethodName());
        map.put("params" + info, node.getParams());
        map.put("returnType" + info, node.getReturnType());
        return map;
    }
}
