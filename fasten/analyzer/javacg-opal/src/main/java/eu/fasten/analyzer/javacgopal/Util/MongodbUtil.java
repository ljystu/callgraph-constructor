package eu.fasten.analyzer.javacgopal.Util;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import org.bson.Document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MongodbUtil {

    private static MongoClient mongo = null;
    private static Properties properties = null;
    private static String host = null;
    private static int port = 0;
    private static int poolSize = 0;
    private static int blockSize = 0;

    // 初始化连接池，设置参数
    static {
        properties = new Properties();
        try {
            properties.load(MongodbUtil.class.getClassLoader().getResourceAsStream("mongo.properties"));
            host = properties.getProperty("host");
            port = Integer.parseInt(properties.getProperty("port"));
            poolSize = Integer.parseInt(properties.getProperty("poolSize"));
            blockSize = Integer.parseInt(properties.getProperty("blockSize"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // 设置连接参数
            ServerAddress serverAddress = new ServerAddress(host, port);
//                MongoOptions mongoOptions = new MongoOptions();
//                mongoOptions.connectionsPerHost = poolSize; // 连接池大小
//                mongoOptions.threadsAllowedToBlockForConnectionMultiplier = blockSize; // 等待队列长度
            // 创建连接对象
            MongoCredential credential = MongoCredential.createScramSha1Credential("admin", "admin", "123456".toCharArray());
            List<MongoCredential> credentials = new ArrayList<MongoCredential>();
            credentials.add(credential);
            mongo = new MongoClient(serverAddress, credentials);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取连接对象
    public static Mongo getMongo() {
        return mongo;
    }

    public static void uploadEdges(HashSet<Edge> allEdges, String artifact) {
        // 创建MongoDB客户端连接

//        ServerAddress serverAddress = new ServerAddress("localhost", 27017);
//        List<ServerAddress> addrs = new ArrayList<ServerAddress>();
//        addrs.add(serverAddress);
//
//
//        MongoCredential credential = MongoCredential.createScramSha1Credential("admin", "admin", "123456".toCharArray());
//        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
//        credentials.add(credential);
//
//
//        //通过连接认证获取MongoDB连接
//        MongoClient mongoClient = new MongoClient(addrs, credentials);

        // 获取MongoDB数据库

        MongoDatabase database = mongo.getDatabase("mydatabase");

        // 获取MongoDB集合
        MongoCollection<Document> collection = database.getCollection("mycollection");

        List<WriteModel<Document>> bulkWrites = new ArrayList<>();

        Pattern excludedPattern = Pattern.compile(readExcludedPackages());
// 在节点name字段上建立唯一索引
//        collection.createIndex(Indexes.ascending("name","type"), new IndexOptions().unique(true));

// 在边的起点和终点字段上建立唯一索引
//        collection.createIndex(Indexes.compoundIndex(Indexes.ascending("startNode"), Indexes.ascending("endNode")), new IndexOptions().unique(true));

        // 创建起始节点的Document
        for (Edge edge : allEdges) {
            GraphNode fromNode = edge.getFrom();
            GraphNode toNode = edge.getTo();
            if (isExcluded(toNode.getPackageName(), excludedPattern)) {
                continue;
            }
            Document startNode = new Document("packageName", fromNode.getPackageName())
                    .append("className", fromNode.getClassName())
                    .append("coordinate", artifact);


            Document endNode = new Document("packageName", toNode.getPackageName())
                    .append("className", toNode.getClassName())
                    .append("coordinate", artifact);

            Document mongoEdge = new Document("startNode", startNode)
                    .append("endNode", endNode).append("type", "static");
            bulkWrites.add(new InsertOneModel<>(mongoEdge));

        }

        try {
            BulkWriteResult result = collection.bulkWrite(bulkWrites);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 关闭MongoDB客户端连接
//        mongo.close();
    }

    private static boolean isExcluded(String definedPackage, Pattern importPattern) {
        Matcher matcher = importPattern.matcher(definedPackage);
        return matcher.matches();
    }

    private static String readExcludedPackages() {
        StringBuilder str = new StringBuilder();
        try {
            Path path = new File("/Users/ljystu/Desktop/neo4j/fasten/analyzer/javacg-opal/src/main/resources/exclusions.txt").toPath();
            String content = Files.readString(path);
            String[] lines = content.split("\r\n");
            for (String line : lines) {
                str.append(line);
                str.append("|");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        str.setLength(str.length() - 1);

        return str.toString();
    }
}
