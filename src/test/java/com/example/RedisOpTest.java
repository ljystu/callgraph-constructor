package com.example;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import ljystu.project.callgraph.util.Neo4jUtil;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisOpTest {
    Jedis jedis;

    Neo4jUtil neo4jUtil;

    public RedisOpTest() {

    }

    public void init() {
        this.neo4jUtil = new Neo4jUtil("bolt://localhost:7687", "neo4j", "ljystu");
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
//            neo4jUtil.upload(edge);
            edges.add(edge);
            nodes.add(edge.getFrom());
            nodes.add(edge.getTo());


//            jedis.del(key);
        }
        List<Node> nodesList = new ArrayList<>();
        nodesList.addAll(nodes);
        neo4jUtil.uploadBatch(nodesList, edges, label);
        jedis.del(label);
        neo4jUtil.close();
        // 关闭Jedis对象
        jedis.close();
    }

}