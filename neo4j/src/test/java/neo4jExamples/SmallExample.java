//package ljystu.project.callgraph.neo4jExamples;
//
//import ljystu.project.callgraph.entity.Node;
//import org.neo4j.driver.AuthTokens;
//import org.neo4j.driver.GraphDatabase;
//import org.neo4j.driver.SessionConfig;
//
//
//import org.neo4j.driver.*;
//
//import static org.neo4j.driver.Values.parameters;
//
//public class SmallExample {
//    // Driver objects are thread-safe and are typically made available application-wide.
//    Driver driver;
//
//    public SmallExample(String uri, String user, String password) {
//        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
//
//    }
//
//    private void addEdge(String method_from, String class1, String method_to, String class2) {
//        try (Session session = driver.session()) {
//
//            session.writeTransaction(tx -> tx.run("MATCH (method_from:Method {name: $method1, class: $class1 }), " +
//                            "(method_to:Method {name: $method2, class: $class2}) " +
//                            "MERGE (method_from)-[r:CALLS]->(method_to)",
//                    parameters("method1", method_from,
//                            "class1", class1, "method2", method_to, "class2", class2)));
////            System.out.println(result.next().toString());
//        }
//    }
//
//    private void addMethod(String name, String clazz) {
//        // Sessions are lightweight and disposable connection wrappers.
//        try (Session session = driver.session()) {
//            // Wrapping a Cypher Query in a Managed Transaction provides atomicity
//            // and makes handling errors much easier.
//            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
//            // These methods are also able to handle connection problems and transient errors using an automatic retry mechanism.
//            session.writeTransaction(tx -> tx.run("MERGE (a:Method {name: $x, class: $class })"
//                    , parameters("x", name, "class", clazz)));
//        }
//    }
//
//    private void printMethods(String initial) {
//        try (Session session = driver.session(SessionConfig.forDatabase("callgraph"))) {
//            // A Managed transaction is a quick and easy way to wrap a Cypher Query.
//            // The `session.run` method will run the specified Query.
//            // This simpler method does not use any automatic retry mechanism.
//            Result result = session.run(
//                    "MATCH (a:Method) WHERE a.packageName STARTS WITH $x RETURN a AS node",
//                    parameters("x", initial));
//            // Each Cypher execution returns a stream of records.
//            while (result.hasNext()) {
//                Value value = result.next().get("node");
//                Node node = new Node();
//                node.setPackageName(value.get("packageName").asString());
//                node.setClassName(value.get("className").asString());
//                node.setMethodName(value.get("methodName").asString());
//                node.setParams(value.get("params").asString());
//                node.setReturnType(value.get("returnType").asString());
//                System.out.println(node);
//                // Values can be extracted from a record by index or name.
////                System.out.println((Node)record.get("packageName"));
//            }
//        }
//    }
//
//    public void close() {
//        // Closing a driver immediately shuts down all open connections.
//        driver.close();
//    }
//
//    public static void main(String... args) {
//        SmallExample example = new SmallExample("bolt://localhost:7687", "neo4j", "ljystu");
////        example.addMethod("addMethod", "mytest.test");
////        example.addMethod("addMethod", "neo4j.driver");
////        example.addMethod("close", "neo4j.driver");
////        example.addMethod("main", "neo4j.driver");
////        example.addEdge("main", "neo4j.driver", "addMethod", "mytest.test");
//        example.printMethods("org.neo4j");
//        example.close();
//    }
//}
