package com.example.neo4jtest;


import org.junit.Test;
import org.neo4j.driver.*;

import java.io.File;
import java.net.URL;

import static org.neo4j.driver.Values.parameters;

public class HelloExampleTest {


    @Test
    public void testAll() throws Exception {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687",
                AuthTokens.basic("neo4j", "ljystu"));
        Session session = driver.session();

        final String message = "Greeting";
        String greeting = session.writeTransaction(
                new TransactionWork<String>() {
                    public String execute(Transaction tx) {
                        Result result = tx.run("CREATE (a:Greeting) " +
                                        "SET a.message = $message " +
                                        "RETURN a.message + ', from node ' + id(a)",
                                parameters("message", message));
                        return result.single().get(0).asString();
                    }
                }
        );

//        System.out.println(greeting);

        System.out.println("OK");
        driver.close();
//        File directory= new File("zookeeper-master").getAbsoluteFile();
//        deleteFile(directory);

//        File dir = new File("/path/to/dir");
//        URL url = dir.toURI().toURL();
//        URL[] urls = new URL[]{url};
//
//        for (URL url1: urls){
//            System.out.println(url1.toString());
//        }

    }

}

