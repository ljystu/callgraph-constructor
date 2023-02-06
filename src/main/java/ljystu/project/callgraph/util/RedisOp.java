package ljystu.project.callgraph.util;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import redis.clients.jedis.Jedis;

import java.util.*;

public class RedisOp {
    Jedis jedis;

    Neo4jUtil neo4jUtil;

    public RedisOp() {
    }

    public void init() {
        this.neo4jUtil = new Neo4jUtil();
        neo4jUtil.init("bolt://localhost:7687", "neo4j", "ljystu");
    }


    public void upload(String label,HashMap<String,String> map) {
        init();
        // 创建Jedis对象，连接到Redis服务器
        jedis = new Jedis("localhost");


        HashSet<Node> nodes = new HashSet<>();
        List<Edge> edges = new ArrayList<>();
        Set<String> dynamic = jedis.smembers("dynamic");
        // 遍历所有键，获取对应的值并删除
        for (String value : dynamic) {
//            String value = jedis.get(key);
            Edge edge = JSON.parseObject(value, Edge.class);
            System.out.println(edge.toString());
            Node nodeFrom = edge.getFrom();
            Node nodeTo = edge.getTo();

//            edges.add(edge);
            getVersion(nodeFrom,nodeTo,map);
            nodes.add(nodeFrom);
            nodes.add(nodeTo);

            edges.add(new Edge(nodeFrom, nodeTo));

        }
        List<Node> nodesList = new ArrayList<>();
        nodesList.addAll(nodes);
        neo4jUtil.uploadBatch(nodesList, edges, label);
        jedis.del("dynamic");
        neo4jUtil.close();
        // 关闭Jedis对象
        jedis.close();
    }

    public void getVersion(Node nodeFrom, Node nodeTo, HashMap<String, String> map) {
        String nodeFromClassName = nodeFrom.getPackageName() + "." + nodeFrom.getClassName();

        String coordinate = CoordinateUtil.getCoordinate(nodeFromClassName, "");
        String nodeFromCoordinate = map.getOrDefault(nodeFromClassName, "not found");
        nodeFrom.setCoordinate(nodeFromCoordinate);
        String nodeToClassName = nodeTo.getPackageName() + "." + nodeTo.getClassName();
        String nodeToCoordinate = map.getOrDefault(nodeToClassName, "not found");
        nodeTo.setCoordinate(nodeToCoordinate);
    }

}