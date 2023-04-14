package ljystu.project.callgraph.uploader;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import ljystu.project.callgraph.utils.MongodbUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import static ljystu.project.callgraph.utils.PackageUtil.packageToCoordMap;

/**
 * The type Redis op.
 *
 * @author ljystu
 */
@Slf4j
public class CallGraphUploader {

    /**
     * The Jedis.
     */
    Jedis jedis;

    /**
     * The Neo 4 j utils.
     */
    Neo4jOp neo4JOp;

    /**
     * Instantiates a new Redis op.
     */
    public CallGraphUploader() {
//        this.neo4JOp = new Neo4jOp(Constants.NEO4J_PORT, Constants.NEO4J_USERNAME, Constants.NEO4J_PASSWORD);
        this.jedis = new Jedis(Constants.SERVER_IP_ADDRESS);
        this.jedis.auth(Constants.REDIS_PASSWORD);
    }

    public void uploadAll(String dependencyCoordinate, String artifactId) {

        upload(artifactId, packageToCoordMap, dependencyCoordinate);
//        upload("static", packageToCoordMap);
//        neo4JOp.close();

        //prevent read timeout
//        jedis.flushAll();
//        jedis.del(artifactId);
        jedis.close();
    }

    /**
     * Upload.
     *
     * @param label             the label
     * @param packageToCoordMap the map
     */
    private void upload(String label, Map<String, String> packageToCoordMap, String dependencyCoordinate) {

//        HashSet<Node> nodes = new HashSet<>();
        HashSet<Edge> edges = new HashSet<>();

        // 遍历所有键，获取对应的值并删除
        for (String value : jedis.smembers(label)) {

            Edge edge;
            try {
                edge = JSON.parseObject(value, Edge.class);
            } catch (Exception e) {
                continue;
            }
            if (edge == null) {
                continue;
            }

            Node nodeFrom = edge.getFrom();
            Node nodeTo = edge.getTo();

            getFullCoordinates(nodeFrom, packageToCoordMap);
            getFullCoordinates(nodeTo, packageToCoordMap);

            if (Objects.equals(nodeFrom.getCoordinate(), null)) {
                nodeFrom.setCoordinate("not found");
            }
            if (Objects.equals(nodeTo.getCoordinate(), null)) {
                nodeTo.setCoordinate("not found");
            }

            if (edge.getFrom().getPackageName().startsWith(Constants.PACKAGE_PREFIX) ||
                    edge.getTo().getPackageName().startsWith(Constants.PACKAGE_PREFIX)) {
                Edge newEdge = new Edge(nodeFrom, nodeTo);
                log.info("Edge upload: " + newEdge);
//                System.out.println("Edge upload: " + newEdge);

//                nodes.add(nodeFrom);
//                nodes.add(nodeTo);

                edges.add(newEdge);
            }

        }

//        List<Node> nodesList = new ArrayList<>(nodes);
//        neo4JOp.uploadAllToNeo4j(nodesList, edges, label);
        MongodbUtil.uploadEdges(edges, dependencyCoordinate);


    }

    private void getFullCoordinates(Node nodeFrom, Map<String, String> packageToCoordMap) {

        String nodeFromMavenCoord = packageToCoordMap.get(nodeFrom.getPackageName());
        if (nodeFromMavenCoord == null) {
            nodeFromMavenCoord = jedis.get(nodeFrom.getPackageName());
        }
        nodeFrom.setCoordinate(nodeFromMavenCoord);

    }

}