package com.example.neo4jtest.neo4jExamples;


import org.neo4j.driver.*;


import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import static org.neo4j.driver.Values.parameters;

public class HelloWorldExample implements AutoCloseable {
    private final Driver driver;

    public HelloWorldExample(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

    }

    @Override
    public void close() throws RuntimeException {
        driver.close();
    }

    public void printGreeting(final String message) {
        try (var session = driver.session()) {
            var greeting = session.executeWrite(tx -> {
                var query = new Query("CREATE (a:Greeting) SET a.message = $message RETURN a.message + ', from node ' + id(a)", parameters("message", message));
                var result = tx.run(query);
                return result.single().get(0).asString();
            });
            System.out.println(greeting);
        }
    }

    public static void main(String... args) {
        try (var greeter = new HelloWorldExample("bolt://localhost:7687", "neo4j", "ljystu")) {
            greeter.printGreeting("hello, world");
        }
    }
}