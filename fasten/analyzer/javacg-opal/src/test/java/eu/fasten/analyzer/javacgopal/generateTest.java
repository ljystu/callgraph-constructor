package eu.fasten.analyzer.javacgopal;

import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class generateTest {
    @Test
    public void generateTest() {
        Main.main(new String[]{"-m", "-a", "/Users/ljystu/Desktop/neo4j/neo4j/junit4-main/target/dependency/hamcrest-core-1.3.jar", "-g", "-i", "FILE", "-o", "/Users/ljystu/Desktop/file.json"});

    }

    @Test
    public void deduplicateTest() {
        HashSet<Edge> set = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            GraphNode graphNode = new GraphNode("1", "1", "1", "1", "1", "1");

            GraphNode toNode = new GraphNode("2", "2", "2", "1", "2", "2");
            Edge edge = new Edge(graphNode, toNode);
            set.add(edge);
        }
        System.out.println(set);
    }
}
