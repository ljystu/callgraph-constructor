package ljystu.project.callgraph;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.util.Neo4jUtil;

import redis.clients.jedis.Jedis;

import java.util.Set;

public class RedisOp {
    Jedis jedis;

    Neo4jUtil neo4jUtil;

    public RedisOp() {
    }

    public void init() {
        this.neo4jUtil = new Neo4jUtil();
        neo4jUtil.init("bolt://localhost:7687", "neo4j", "ljystu");
    }


    public void upload() {
        init();
        // 创建Jedis对象，连接到Redis服务器
        jedis = new Jedis("localhost");

        // 获取所有键
        Set<String> keys = jedis.keys("*");

        // 遍历所有键，获取对应的值并删除
        for (String key : keys) {
            String value = jedis.get(key);
            Edge edge = JSON.parseObject(value, Edge.class);
            neo4jUtil.upload(edge);

            jedis.del(key);
        }
        neo4jUtil.close();
        // 关闭Jedis对象
        jedis.close();
    }

}