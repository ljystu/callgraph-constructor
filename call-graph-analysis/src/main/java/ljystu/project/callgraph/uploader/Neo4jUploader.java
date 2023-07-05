package ljystu.project.callgraph.uploader;

import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import ljystu.project.callgraph.utils.MongodbUtils;
import org.neo4j.driver.*;

import java.util.*;

import static org.neo4j.driver.Values.parameters;


/**
 * The type Neo 4 j utils.
 */
public class Neo4jUploader {

    Driver driver;

    public Neo4jUploader() {

    }

    public Neo4jUploader(String uri, String user, String password) {
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
            session.writeTransaction(tx -> tx.run("MATCH (method_from:Method {packageName: $packagename, className: $classname, " + "methodName: $methodname, params: $params, returnType: $returntype }), " + "(method_to:Method {packageName: $packagename2, className: $classname2, " + "methodName: $methodname2, params: $params2, returnType: $returntype2}) " + "MERGE (method_from)-[r:CALLS]->(method_to)", parameters("packagename", from.getPackageName(), "classname", from.getClassName(), "methodname", from.getMethodName(), "params", from.getParams(), "returntype", from.getReturnType(), "packagename2", to.getPackageName(), "classname2", to.getClassName(), "methodname2", to.getMethodName(), "params2", to.getParams(), "returntype2", to.getReturnType())));
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
            session.writeTransaction(tx -> tx.run("UNWIND $edgeNodePairs as row " + "MERGE (method_from:Class {packageName: row.packageName, className: row.className, " +
//                            "methodName: row.methodName, params: row.params, returnType: row.returnType, " +
                    "coordinate: row.dependency }) " + "MERGE(method_to:Class {packageName: row.packageName2, className: row.className2, " +
//                            "methodName: row.methodName2, params: row.params2, returnType: row.returnType2, " +
                    "coordinate: row.dependency2 }) " + "MERGE (method_from)-[r:Call]->(method_to)" + "ON CREATE SET r.type = $type \n" + "ON MATCH SET r.type = \n" + "CASE r.type\n" + "   WHEN $type \n" + "   THEN $type \n" + "   ELSE 'both' \n" + "END", parameters));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    private void addBatchEdge(List<Map<String, Object>> edgeNodePairs, String type) {
////        Map<String, Object> parameters = new HashMap<>();
////        parameters.put("items", edgeNodePairs);
////        parameters.put("type", type);
//
//        String query = "UNWIND $dataList AS row return row";
//        String action =
//                "MERGE (method_from:Class {packageName: row.packageName, className: row.className, returnType: row.returnType, coordinate: row.dependency }) " +
//                        "MERGE (method_to:Class {packageName: row.packageName2, className: row.className2, returnType: row.returnType2, coordinate: row.dependency2 }) " +
//                        "MERGE (method_from)-[r:Call]->(method_to) " +
//                        "ON CREATE SET r.type = $type " +
//                        "ON MATCH SET r.type = " +
//                        "CASE r.type " +
//                        "   WHEN $type " +
//                        "   THEN $type " +
//                        "   ELSE 'both' " +
//                        "END";
//
//        Map<String, Object> parameters = new HashMap<>();
//        parameters.put("dataList", edgeNodePairs);
//        parameters.put("type", "your_type_value");
//
//        String iterateQuery = "CALL apoc.periodic.iterate($query,$action, $queryBatchParams)";
//
//        Map<String, Object> queryBatchParams = new HashMap<>();
//        queryBatchParams.put("batchSize", 1000);
//        queryBatchParams.put("parallel", true);
//        queryBatchParams.put("iterateList", true);
//        queryBatchParams.put("params", parameters);
//
//        try (Session session = driver.session(SessionConfig.forDatabase("test"))) {
//            Result result = session.run(iterateQuery, ImmutableMap.of("query", query, "action", action, "queryBatchParams", queryBatchParams));
//            // process the result as needed
//        } catch (
//                Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    public void uploadFromMongo(String coord) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("coord", coord);
        try (Session session = driver.session(SessionConfig.forDatabase(Constants.DATABASE))) {
            session.writeTransaction(tx -> tx.run("CALL apoc.mongodb.find('mongodb://admin:123456@localhost:27017', 'mydatabase', 'mycollection', { `startNode.coordinate`: $coord },{},{}) YIELD value\n" + "UNWIND value AS edgeData\n" + "WITH edgeData.startNode AS startNode, edgeData.endNode AS endNode, edgeData.type AS type\n" + "MERGE (start:Class {packageName: startNode.packageName, className: startNode.className, coordinate: startNode.coordinate})\n" + "MERGE (end:Class {packageName: endNode.packageName, className: endNode.className, coordinate: endNode.coordinate})\n" + "MERGE (start)-[r:Call]->(end)\n" + "ON CREATE SET r.type = type\n" + "ON MATCH SET r.type = \n" + "CASE r.type\n" + "WHEN type \n" + "THEN type \n" + "ELSE 'both'\n" + "END ", parameters));
            MongodbUtils.deleteEdges(coord);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addVulnerableLabel(Map<String, Object> map, String label) {

        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session(SessionConfig.forDatabase(label))) {
            // Wrapping a Cypher Query in a Managed Transaction provides atomicity
            // and makes handling errors much easier.
            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
            // These methods are also able to handle connection problems and transient errors using an automatic retry mechanism.
            session.writeTransaction(tx -> tx.run("match (a:Method {packageName: $packageName, className: $className," + " methodName: $methodName, params: $params, returnType: $returnType , coordinate : $coordinate }) <-[*]-(callingNode) " + " Set callingNode:VulnerableMethod", parameters("packageName", map.get("packageName"), "className", map.get("className"), "methodName", map.get("methodName"), "params", map.get("params"), "returnType", map.get("returnType"), "coordinate", map.get("coordinate"))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBatchVulnerableLabel(List<Map<String, Object>> nodeList, String label) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("batches", nodeList);

        try (Session session = driver.session(SessionConfig.forDatabase(label))) {
            session.writeTransaction(tx -> tx.run("UNWIND $batches as row " + "MATCH (n:Method {packageName: row.packageName, className: row.className," + " methodName: row.methodName, params: row.params, returnType: row.returnType, " + "coordinate: row.coordinate}) Set n:OriginalVulnerableMethod", parameters));
//            session.writeTransaction(tx -> tx.run("UNWIND $batches as row " +
//                            "MATCH (n:Method {packageName: row.packageName, className: row.className," +
//                            " methodName: row.methodName, params: row.params, returnType: row.returnType, " +
//                            "coordinate: row.coordinate})<-[*]-(callingNode) " +
//                            "SET callingNode:VulnerableMethod",
//                    parameters));
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
            session.writeTransaction(tx -> tx.run("MERGE (a:Method {packageName: $packagename, className: $classname," + " methodName: $methodname, params: $params, returnType: $returntype })", parameters("packagename", packageName, "classname", className, "methodname", methodName, "params", params, "returntype", returnType)));
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
            session.writeTransaction(tx -> tx.run("UNWIND $batches as row " + "MERGE (a:Class {packageName: row.packageName, className: row.className," +
//                            " methodName: row.methodName, params: row.params, returnType: row.returnType, " +
                            "coordinate: row.dependency })",

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
    public void uploadAllToNeo4j(List<Node> nodesList, Set<Edge> edges, String label) {
//        uploadMethodNodes(nodesList);
        uploadEdges(edges, label);
//        HashSet<String> allCoords = MongodbUtils.findAllCoords();
//        for (String coord : allCoords) {
//            uploadFromMongo(coord);
//        }
    }

    private void uploadEdges(Set<Edge> edges, String label) {
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
