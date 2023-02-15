package com.example;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import ljystu.project.callgraph.uploader.Neo4jOp;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CallGraphUploaderTest {
    Jedis jedis;

    Neo4jOp neo4JOp;

    public CallGraphUploaderTest() {

    }

    public void init() {
        this.neo4JOp = new Neo4jOp("bolt://localhost:7687", "neo4j", "ljystu");
    }


    @Test
    public void upload() {
        init();
        // 创建Jedis对象，连接到Redis服务器
        jedis = new Jedis("localhost");

        String label = "static";
        // 获取所有键
//        Set<String> keys = jedis.keys("*");
        HashSet<Node> nodes = new HashSet<>();
        List<Edge> edges = new ArrayList<>();
        Set<String> dynamic = jedis.smembers(label);
        // 遍历所有键，获取对应的值并删除
        for (String value : dynamic) {
//            String value = jedis.get(key);
            Edge edge = JSON.parseObject(value, Edge.class);
            System.out.println(edge.toString());
//            neo4JOp.upload(edge);
            edges.add(edge);
            nodes.add(edge.getFrom());
            nodes.add(edge.getTo());


//            jedis.del(key);
        }
        List<Node> nodesList = new ArrayList<>();
        nodesList.addAll(nodes);
        neo4JOp.uploadAllToNeo4j(nodesList, edges, label);
        jedis.del(label);
        neo4JOp.close();
        // 关闭Jedis对象
        jedis.close();
    }

}