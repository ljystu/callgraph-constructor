package com.example;

import com.alibaba.fastjson.JSON;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.myEdge;
import ljystu.project.callgraph.uploader.Neo4jOp;
import ljystu.project.callgraph.utils.MongodbUtil;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class MongodbTest {


    private static MongoClient mongo = null;
    @Before
    public void init() {
//        mongo = MongodbUtil.getMongoClient();
    }


    public static void main(String[] args) {
        // 创建MongoDB客户端连接

        ServerAddress serverAddress = new ServerAddress("localhost", 27017);
        List<ServerAddress> addrs = new ArrayList<ServerAddress>();
        addrs.add(serverAddress);


        MongoCredential credential = MongoCredential.createScramSha1Credential("admin", "admin", "123456".toCharArray());
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        credentials.add(credential);


        //通过连接认证获取MongoDB连接
        MongoClient mongoClient = new MongoClient(addrs, credentials);


        // 获取MongoDB数据库
        MongoDatabase database = mongoClient.getDatabase("mydatabase");

        // 获取MongoDB集合
        MongoCollection<Document> collection = database.getCollection("mycollection");

        List<WriteModel<Document>> bulkWrites = new ArrayList<>();


// 在节点name字段上建立唯一索引
//        collection.createIndex(Indexes.ascending("name","type"), new IndexOptions().unique(true));

// 在边的起点和终点字段上建立唯一索引
        collection.createIndex(Indexes.compoundIndex(Indexes.ascending("startNode"), Indexes.ascending("endNode")), new IndexOptions().unique(true));

        // 创建起始节点的Document
        Document startNode = new Document("name", "StartNode8")
                .append("type", "Node");
        Document startNode2 = new Document("name", "StartNode")
                .append("type", "Node");

        // 创建结束节点的Document
        Document endNode = new Document("name", "EndNode4")
                .append("type", "Node3");

        // 创建边的Document
        Document edge = new Document("startNode", startNode)
                .append("endNode", endNode)
                .append("type", "Edge")
                .append("weight", 1.0);

        Document edge2 = new Document("startNode", startNode2)
                .append("endNode", endNode)
                .append("type", "Edge")
                .append("weight", 1.0);

//         将节点和边的信息存储到MongoDB中
//        collection.insertMany(Arrays.asList(edge,edge2));

        InsertOneModel<Document> insertOneModel = new InsertOneModel<>(edge);
        InsertOneModel<Document> insertOneModel2 = new InsertOneModel<>(edge2);
        bulkWrites.add(insertOneModel);
        bulkWrites.add(insertOneModel2);
        try {
            BulkWriteResult result = collection.bulkWrite(bulkWrites);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 关闭MongoDB客户端连接
        mongoClient.close();
    }

    @Test
    public void uploadTest() {
        Neo4jOp neo4jOp = new Neo4jOp("bolt://localhost:7687", "neo4j", "ljystuneo");
        HashSet<String> allCoords =findAllCoords();
        for (String coord : allCoords) {
            neo4jOp.uploadFromMongo(coord);
        }
    }
    public static HashSet<String> findAllCoords() {
        HashSet<String> set = new HashSet<>();
        MongoCollection<Document> collection = mongo.getDatabase("mydatabase").getCollection("mycollection");
        collection.distinct("startNode.coordinate", String.class).into(set);
        return set;
    }

    @Test
    public void mongoUpdateType() {
        ServerAddress serverAddress = new ServerAddress("34.140.10.226", 27017);
        List<ServerAddress> addrs = new ArrayList<ServerAddress>();
        addrs.add(serverAddress);

        MongoCredential credential = MongoCredential.createScramSha1Credential("admin", "admin", "123456".toCharArray());
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        credentials.add(credential);

        //通过连接认证获取MongoDB连接
        MongoClient mongoClient = new MongoClient(addrs, credentials);


        // 获取MongoDB数据库
        MongoDatabase database = mongoClient.getDatabase("mydatabase");

        // 获取MongoDB集合
        MongoCollection<Document> collection = database.getCollection("org.apache.zookeeper:zookeeper");

        List<WriteModel<Document>> bulkWrites = new ArrayList<>();

        Document startNode = new Document("packageName", "org.apache.zookeeper.server")
                .append("className", "ConnectionBean")
                .append("coordinate", "org.apache.zookeeper:zookeeper:3.6.0");

        Document endNode = new Document("packageName", "org.apache.zookeeper.server")
                .append("className", "ZKDatabase")
                .append("coordinate", "org.apache.zookeeper:zookeeper:3.6.0");

        String type = "static";

        Bson filter = Filters.and(Filters.eq("startNode", startNode),
                Filters.eq("endNode", endNode),
                Filters.ne("type", type));

        Document existingDocument = collection.find(filter).first();

        if (existingDocument != null) {
            // 已经存在具有相同startNode和endNode，但具有不同type的文档，更新type为both
            Bson update = new Document("$set", new Document("type", "both"));
            bulkWrites.add(new UpdateOneModel<>(filter, update));
//            collection.updateOne(filter, update);
        } else {
            // 插入新文档
            Document newDocument = new Document("startNode", startNode)
                    .append("endNode", endNode)
                    .append("type", type);
//            bulkWrites.add(new UpdateOneModel<>(newDocument));
            collection.insertOne(newDocument);
        }
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        collection.bulkWrite(bulkWrites, options);

        mongoClient.close();
    }

}
