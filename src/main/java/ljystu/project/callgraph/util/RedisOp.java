package ljystu.project.callgraph.util;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.*;

@Slf4j
public class RedisOp {
    Jedis jedis;

    Neo4jUtil neo4jUtil;

    public RedisOp() {
        this.neo4jUtil = new Neo4jUtil();
        neo4jUtil.init("bolt://localhost:7687", "neo4j", "ljystu");
    }

    public void upload(String label, Map<String, String> map) {

        // 创建Jedis对象，连接到Redis服务器
        jedis = new Jedis("localhost");

        HashSet<Node> nodes = new HashSet<>();
        List<Edge> edges = new ArrayList<>();

        Set<String> dynamic = jedis.smembers(label);
        // 遍历所有键，获取对应的值并删除
        for (String value : dynamic) {

            Edge edge = JSON.parseObject(value, Edge.class);
            log.debug("Edge upload" + edge.toString());
            Node nodeFrom = edge.getFrom();
            Node nodeTo = edge.getTo();

            getFullCoordinates(nodeFrom, nodeTo, map);
            if (nodeFrom.getCoordinate() == "" || nodeTo.getCoordinate() == "") continue;

            nodes.add(nodeFrom);
            nodes.add(nodeTo);

            edges.add(new Edge(nodeFrom, nodeTo));
        }

        List<Node> nodesList = new ArrayList<>(nodes);
        neo4jUtil.uploadBatch(nodesList, edges, label);
        jedis.del(label);

        neo4jUtil.close();
        jedis.close();
    }

    private void getFullCoordinates(Node nodeFrom, Node nodeTo, Map<String, String> map) {
        // TODO 需要version 来建立mapping
        String nodeFromClassName = nodeFrom.getPackageName() + "." + nodeFrom.getClassName();
        String fromCoordinate = CoordinateUtil.getCoordinate(nodeFromClassName, "");
        String nodeFromCoordinate = map.getOrDefault(fromCoordinate, "");
        nodeFrom.setCoordinate(nodeFromCoordinate);

        String nodeToClassName = nodeTo.getPackageName() + "." + nodeTo.getClassName();
        String toCoordinate = CoordinateUtil.getCoordinate(nodeToClassName, "");
        String nodeToCoordinate = map.getOrDefault(toCoordinate, "");
        nodeTo.setCoordinate(nodeToCoordinate);
    }

}