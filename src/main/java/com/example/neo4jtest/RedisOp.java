package com.example.neo4jtest;

import com.alibaba.fastjson.JSON;
import com.example.neo4jtest.entity.Edge;
import com.example.neo4jtest.util.Neo4jUtil;

import redis.clients.jedis.Jedis;

import java.util.Set;

public class RedisOp {
    static Jedis jedis;

    static Neo4jUtil neo4jUtil;

    public static void init() {
        Neo4jUtil.init("bolt://localhost:7687", "neo4j", "ljystu");
    }


    public static void upload() {
        init();
        // 创建Jedis对象，连接到Redis服务器
        jedis = new Jedis("localhost");

        // 获取所有键
        Set<String> keys = jedis.keys("*");

        // 遍历所有键，获取对应的值并删除
        for (String key : keys) {
            String value = jedis.get(key);
            Edge edge = JSON.parseObject(value, Edge.class);
            Neo4jUtil.upload(edge);

            jedis.del(key);
        }
        Neo4jUtil.close();
        // 关闭Jedis对象
        jedis.close();
    }





}