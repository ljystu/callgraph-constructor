package ljystu.project.callgraph.uploader;


import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import org.neo4j.driver.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;


/**
 * The type Neo 4 j utils.
 */
public class Neo4jOp {

    Driver driver;


    public Neo4jOp() {

    }


    public Neo4jOp(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

    }

    /**
     * Add edge.
     *
     * @param edge the edge
     */
    @Deprecated
    private void addEdge(Edge edge) {
        try (Session session = driver.session(SessionConfig.forDatabase(Constants.DATABASE))) {
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

    /**
     * Add edge unwind.
     *
     * @param edgeNodePairs the edge node pairs
     * @param type          the type
     */
    private void addBatchEdge(List<Map<String, Object>> edgeNodePairs, String type) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("edgeNodePairs", edgeNodePairs);
        parameters.put("type", type);

        try (Session session = driver.session(SessionConfig.forDatabase(Constants.DATABASE))) {
            session.writeTransaction(tx -> tx.run("UNWIND $edgeNodePairs as row " +
                            "MATCH (method_from:Method {packageName: row.packageName, className: row.className, " +
                            "methodName: row.methodName, params: row.params, returnType: row.returnType, dependency: row.dependency }), " +
                            "(method_to:Method {packageName: row.packageName2, className: row.className2, " +
                            "methodName: row.methodName2, params: row.params2, returnType: row.returnType2, dependency: row.dependency2 }) " +
                            "MERGE (method_from)-[r:CALL]->(method_to)" +
                            "ON CREATE SET r.type = $type \n" +
                            "ON MATCH SET r.type = \n" +
                            "CASE r.type\n" +
                            "   WHEN $type \n" +
                            "   THEN $type \n" +
                            "   ELSE 'both' \n" +
                            "END",
                    parameters));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add method.
     *
     * @param node the node
     */
    @Deprecated
    private void addMethod(Node node) {
        String packageName = node.getPackageName();
        String className = node.getClassName();
        String methodName = node.getMethodName();
        String params = node.getParams();
        String returnType = node.getReturnType();
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session(SessionConfig.forDatabase(Constants.DATABASE))) {
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

    /**
     * Add method unwind.
     *
     * @param nodeList the node list
     */
    private void addBatchMethod(List<Map<String, Object>> nodeList) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batches", nodeList);

        try (Session session = driver.session(SessionConfig.forDatabase(Constants.DATABASE))) {
            session.writeTransaction(tx -> tx.run("UNWIND $batches as row " +
                            "MERGE (a:Method {packageName: row.packageName, className: row.className," +
                            " methodName: row.methodName, params: row.params, returnType: row.returnType, dependency: row.dependency })",
                    parameters));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close.
     */
    public void close() {
        driver.close();
    }

    /**
     * Upload batch.
     *
     * @param nodesList the nodes list
     * @param edges     the edges
     * @param label     the label
     */
    public void uploadAllToNeo4j(List<Node> nodesList, List<Edge> edges, String label) {
        uploadMethodNodes(nodesList);
        uploadEdges(edges, label);
    }

    private void uploadEdges(List<Edge> edges, String label) {
        List<Map<String, Object>> edgeNodePairs = new ArrayList<>();

        for (Edge edge : edges) {
            Node from = edge.getFrom();
            Node to = edge.getTo();

            Map<String, Object> nodeInfo = getNodeInfo(from, "");
            nodeInfo.putAll(getNodeInfo(to, "2"));
            edgeNodePairs.add(nodeInfo);
        }
        addBatchEdge(edgeNodePairs, label);
    }

    private void uploadMethodNodes(List<Node> nodesList) {
        List<Map<String, Object>> nodeMap = new ArrayList<>();
        for (Node node : nodesList) {
            nodeMap.add(getNodeInfo(node, ""));
        }
        addBatchMethod(nodeMap);
    }

    /**
     * Gets node info.
     *
     * @param node the node
     * @param info the info
     * @return the node info
     */
    public Map<String, Object> getNodeInfo(Node node, String info) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("packageName" + info, node.getPackageName());
        map.put("className" + info, node.getClassName());
        map.put("methodName" + info, node.getMethodName());
        map.put("params" + info, node.getParams());
        map.put("returnType" + info, node.getReturnType());
        map.put("dependency" + info, node.getCoordinate());
        return map;
    }
}
