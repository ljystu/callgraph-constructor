package com.example.neo4jtest.util;


import com.example.neo4jtest.entity.Edge;
import com.example.neo4jtest.entity.Node;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;

import java.util.HashSet;

import static org.neo4j.driver.Values.parameters;

public class Neo4jUtil {
    // Driver objects are thread-safe and are typically made available application-wide.
    static Driver driver;

    public static void init(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

    }

    public static void addEdge(Edge edge) {
        try (Session session = driver.session(SessionConfig.forDatabase("callgraph"))) {
            Node from = edge.getFrom();
            Node to = edge.getTo();
            session.writeTransaction(tx -> tx.run("MATCH (method_from:Method {packageName: $packagename, className: $classname, " +
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

    public static void addMethod(Node node) {
        String packageName = node.getPackageName();
        String className = node.getClassName();
        String methodName = node.getMethodName();
        String params = node.getParams();
        String returnType = node.getReturnType();
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session(SessionConfig.forDatabase("callgraph"))) {
            // Wrapping a Cypher Query in a Managed Transaction provides atomicity
            // and makes handling errors much easier.
            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
            // These methods are also able to handle connection problems and transient errors using an automatic retry mechanism.
            session.writeTransaction(tx -> tx.run("MERGE (a:Method {packageName: $packagename, className: $classname," +
                            " methodName: $methodname, params: $params, returnType: $returntype })",
                    parameters("packagename", packageName, "classname", className,
                            "methodname", methodName, "params", params, "returntype", returnType)));
        }
    }

    public void printMethods(String initial) {
        try (Session session = driver.session(SessionConfig.forDatabase("callgraph"))) {
            // A Managed transaction is a quick and easy way to wrap a Cypher Query.
            // The `session.run` method will run the specified Query.
            // This simpler method does not use any automatic retry mechanism.
            Result result = session.run(
                    "MATCH (a:Method) WHERE a.name STARTS WITH $x RETURN a.name AS name",
                    parameters("x", initial));
            // Each Cypher execution returns a stream of records.
            while (result.hasNext()) {
                Record record = result.next();
                // Values can be extracted from a record by index or name.
                System.out.println(record.get("name").asString());
            }
        }
    }

    public static void close() {
        // Closing a driver immediately shuts down all open connections.
        driver.close();
    }

    public  void upload(HashSet<Edge> edges, HashSet<Node> Nodes) {
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

    public static void upload(Edge edge) {
//        init("bolt://localhost:7687", "neo4j", "ljystu");
        addMethod(edge.getFrom());
        addMethod(edge.getTo());
        addEdge(edge);
//        close();
    }
}
