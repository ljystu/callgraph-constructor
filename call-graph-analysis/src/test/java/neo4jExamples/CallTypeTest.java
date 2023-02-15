package neo4jExamples;

import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import ljystu.project.callgraph.uploader.Neo4jOp;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;

public class CallTypeTest {
    @Test
    public void uploadCall() {
        Node nodeFrom = new Node("com.example", "ljystu", "callTest", "int", "void");
        nodeFrom.setCoordinate("my:test:1.0");
        Node nodeTo = new Node("com.delft", "hello", "receiveTest", "int", "void");
        nodeTo.setCoordinate("your:test:1.0");
        Edge edge = new Edge(nodeFrom, nodeTo);
        Neo4jOp neo4jOp = new Neo4jOp("bolt://localhost:7687", "neo4j", "ljystu");
        List<Node> nodeList = asList(nodeFrom, nodeTo);
        List<Edge> edges = asList(edge);
        neo4jOp.uploadAllToNeo4j(nodeList, edges, "static");
        neo4jOp.uploadAllToNeo4j(nodeList, edges, "dynamic");
    }

}
