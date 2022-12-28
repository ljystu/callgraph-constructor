package com.example.neo4jtest;

import org.neo4j.driver.*;
import static org.neo4j.driver.Values.parameters;

public class HelloExample {

    public static void main(String[] args) throws Exception {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687",
                AuthTokens.basic("neo4j", "ljystu"));
        Session session = driver.session();

        final String message = "Greeting";
        String greeting = session.writeTransaction(
                new TransactionWork<String>(){
                    public String execute(Transaction tx ) {
                        Result result = tx.run("CREATE (a:Greeting) " +
                                        "SET a.message = $message " +
                                        "RETURN a.message + ', from node ' + id(a)",
                                parameters( "message", message));
                        return result.single().get( 0 ).asString();
                    }
                }
        );

        System.out.println(greeting);

        System.out.println("OK");
        driver.close();
    }
}
