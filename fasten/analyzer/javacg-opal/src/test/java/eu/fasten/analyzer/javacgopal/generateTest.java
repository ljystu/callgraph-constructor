package eu.fasten.analyzer.javacgopal;

import org.junit.jupiter.api.Test;

public class generateTest {
    @Test
    public void generateTest() {
        Main.main(new String[]{"-m", "-a", "/Users/ljystu/Desktop/neo4j/neo4j/junit4-main/target/dependency/hamcrest-core-1.3.jar", "-g", "-i", "FILE", "-o", "/Users/ljystu/Desktop/file.json"});

    }
}
