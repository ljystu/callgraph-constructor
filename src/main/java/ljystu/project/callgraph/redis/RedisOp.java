package ljystu.project.callgraph.redis;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.Neo4j.Neo4jOp;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * The type Redis op.
 */
@Slf4j
public class RedisOp {
    /**
     * The Jedis.
     */
    Jedis jedis;

    /**
     * The Neo 4 j util.
     */
    Neo4jOp neo4JOp;

    /**
     * Instantiates a new Redis op.
     */
    public RedisOp() {
        this.neo4JOp = new Neo4jOp("bolt://localhost:7687", "neo4j", "ljystu");
    }

    /**
     * Upload.
     *
     * @param label the label
     * @param map   the map
     */
    public void upload(String label, Map<String, String> map) {

        // 创建Jedis对象，连接到Redis服务器
        jedis = new Jedis("localhost");

        HashSet<Node> nodes = new HashSet<>();
        List<Edge> edges = new ArrayList<>();

        Set<String> dynamic = jedis.smembers(label);
        // 遍历所有键，获取对应的值并删除
        for (String value : dynamic) {

            Edge edge = JSON.parseObject(value, Edge.class);
            log.debug("Edge upload：" + edge.toString());
            Node nodeFrom = edge.getFrom();
            Node nodeTo = edge.getTo();

            getFullCoordinates(nodeFrom, nodeTo, map);
            if (Objects.equals(nodeFrom.getCoordinate(), "") || Objects.equals(nodeTo.getCoordinate(), "")) continue;

            nodes.add(nodeFrom);
            nodes.add(nodeTo);

            edges.add(new Edge(nodeFrom, nodeTo));
        }

        List<Node> nodesList = new ArrayList<>(nodes);
        neo4JOp.uploadBatch(nodesList, edges, label);
        jedis.del(label);

        neo4JOp.close();
        jedis.close();
    }


    private void getFullCoordinates(Node nodeFrom, Node nodeTo, Map<String, String> map) {
        String nodeFromMavenCoord = map.get(nodeFrom.getPackageName());
        nodeFrom.setCoordinate(nodeFromMavenCoord);

        String nodeToMavenCoord = map.get(nodeTo.getPackageName());
        nodeTo.setCoordinate(nodeToMavenCoord);
    }

}