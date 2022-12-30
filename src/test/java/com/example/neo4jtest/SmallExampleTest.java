//package com.example.neo4jtest;
//
//import org.junit.Test;
//import org.neo4j.driver.Record;
//import org.neo4j.driver.*;
//
//import static org.neo4j.driver.Values.parameters;
//
//
//public class SmallExampleTest {
//    // Driver objects are thread-safe and are typically made available application-wide.
//    Driver driver;
//
//
//    public void init(String uri, String user, String password) {
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
//        try (Session session = driver.session()) {
//            // A Managed transaction is a quick and easy way to wrap a Cypher Query.
//            // The `session.run` method will run the specified Query.
//            // This simpler method does not use any automatic retry mechanism.
//            Result result = session.run(
//                    "MATCH (a:Method) WHERE a.name STARTS WITH $x RETURN a.name AS name",
//                    parameters("x", initial));
//            // Each Cypher execution returns a stream of records.
//            while (result.hasNext()) {
//                Record record = result.next();
//                // Values can be extracted from a record by index or name.
////                System.out.println(record.get("name").asString());
//            }
//        }
//    }
//
//    public void close() {
//        // Closing a driver immediately shuts down all open connections.
//        driver.close();
//    }
//
//    @Test
//    public void testExample() {
//        SmallExampleTest example = new SmallExampleTest();
//        example.init("bolt://localhost:7687", "neo4j", "ljystu");
////        example.addMethod("addMethod", "mytest.test");
////        example.addMethod("addMethod", "neo4j.driver");
////        example.addMethod("close", "neo4j.driver");
////        example.addMethod("main", "neo4j.driver");
//        example.addEdge("max", "neo4j.driver", "addmy", "mytest.test");
////        example.printMethods("c");
//        example.close();
//    }
//}
