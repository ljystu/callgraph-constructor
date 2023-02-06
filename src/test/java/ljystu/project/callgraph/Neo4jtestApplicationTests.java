//package com.example.neo4jtest;
//
//
//import test.ljystu.project.callgraph.ExampleCommandLineRunner;
//import org.junit.Test;
//import org.neo4j.driver.AuthTokens;
//import org.neo4j.driver.Config;
//import org.neo4j.driver.GraphDatabase;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//class Neo4jtestApplicationTests {
//
//    @Test
//    void TestAll() {
//        var uri = "neo4j+s://a752bb08.databases.neo4j.io";
//
//        var user = "<Username for Neo4j Aura instance>";
//        var password = "<Password for Neo4j Aura instance>";
//
//        try (var app = new ExampleCommandLineRunner(uri, user, password, Config.defaultConfig())) {
//            app.createFriendship("Alice", "David");
//            app.findPerson("Alice");
//        }
//    }
//
//}
