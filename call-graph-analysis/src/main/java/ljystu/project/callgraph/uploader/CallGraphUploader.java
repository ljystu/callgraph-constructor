package ljystu.project.callgraph.uploader;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.*;

import static ljystu.project.callgraph.utils.PackageUtil.packageToCoordMap;

/**
 * The type Redis op.
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
        this.neo4JOp = new Neo4jOp(Constants.NEO4J_PORT, Constants.NEO4J_USERNAME, Constants.NEO4J_PASSWORD);
        this.jedis = new Jedis(Constants.REDIS_ADDRESS);
    }

    public void uploadAll() {

        upload("dynamic", packageToCoordMap);
//        upload("static", packageToCoordMap);
        neo4JOp.close();
        jedis.close();
    }

    /**
     * Upload.
     *
     * @param label the label
     * @param map   the map
     */
    private void upload(String label, Map<String, String> map) {

        HashSet<Node> nodes = new HashSet<>();
        HashSet<Edge> edges = new HashSet<>();

        // 遍历所有键，获取对应的值并删除
        for (String value : jedis.smembers(label)) {

            Edge edge = JSON.parseObject(value, Edge.class);
            log.debug("Edge upload：" + edge.toString());
            Node nodeFrom = edge.getFrom();
            Node nodeTo = edge.getTo();

            getFullCoordinates(nodeFrom, nodeTo, map);
            if (Objects.equals(nodeFrom.getCoordinate(), null) || Objects.equals(nodeTo.getCoordinate(), null))
                continue;

            nodes.add(nodeFrom);
            nodes.add(nodeTo);

            edges.add(new Edge(nodeFrom, nodeTo));
        }

        List<Node> nodesList = new ArrayList<>(nodes);
        neo4JOp.uploadAllToNeo4j(nodesList, edges, label);
        jedis.del(label);

    }


    private void getFullCoordinates(Node nodeFrom, Node nodeTo, Map<String, String> map) {
        String nodeFromMavenCoord = map.get(nodeFrom.getPackageName());
        if (nodeFromMavenCoord != null) {
            nodeFrom.setCoordinate(nodeFromMavenCoord);
        }
        String nodeToMavenCoord = map.get(nodeTo.getPackageName());
        if (nodeToMavenCoord != null) {
            nodeTo.setCoordinate(nodeToMavenCoord);
        }
    }

}