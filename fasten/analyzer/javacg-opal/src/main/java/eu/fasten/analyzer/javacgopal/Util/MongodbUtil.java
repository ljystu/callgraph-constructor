package eu.fasten.analyzer.javacgopal.Util;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import eu.fasten.analyzer.javacgopal.Constants;
import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The type Mongodb util.
 */
public class MongodbUtil {

    private static MongoClient mongo = null;
    private static Properties properties = null;
    private static String host = null;
    private static int port = 0;
    private static int poolSize = 0;
    private static int blockSize = 0;
    private static int edgeCount = 0;
    private static int collectionCount = 0;

    // 初始化连接池，设置参数
//    static {
//        try {
//            // 设置连接参数
//            ServerAddress serverAddress = new ServerAddress(Constants.MONGO_ADDRESS, 27017);
////                MongoOptions mongoOptions = new MongoOptions();
////                mongoOptions.connectionsPerHost = poolSize; // 连接池大小
////                mongoOptions.threadsAllowedToBlockForConnectionMultiplier = blockSize; // 等待队列长度
//            // 创建连接对象
//            MongoCredential credential = MongoCredential.createScramSha1Credential("ljystu", "admin", "Ljystu110!".toCharArray());
//            List<MongoCredential> credentials = new ArrayList<MongoCredential>();
//            credentials.add(credential);
//            mongo = new MongoClient(serverAddress, credentials);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Gets mongo.
     *
     * @return the mongo
     */
// 获取连接对象
    public static Mongo getMongo() {
        return mongo;
    }

    /**
     * Upload edges.
     *
     * @param allEdges the all edges
     */
    public static void uploadEdges(HashSet<Edge> allEdges, String artifact) {
        if (mongo == null) {
            ServerAddress serverAddress = new ServerAddress(Constants.MONGO_ADDRESS, 27017);
//                MongoOptions mongoOptions = new MongoOptions();
//                mongoOptions.connectionsPerHost = poolSize; // 连接池大小
//                mongoOptions.threadsAllowedToBlockForConnectionMultiplier = blockSize; // 等待队列长度
            // 创建连接对象
            MongoCredential credential = MongoCredential.createScramSha1Credential("ljystu", "admin", "Ljystu110!".toCharArray());
            List<MongoCredential> credentials = new ArrayList<MongoCredential>();
            credentials.add(credential);
            mongo = new MongoClient(serverAddress, credentials);
        }
        if (allEdges.isEmpty()) {
            return;
        }

        artifact = artifact.substring(0, artifact.lastIndexOf(":"));
        MongoDatabase database = mongo.getDatabase("mydatabase");

        MongoCollection<Document> collection = database.getCollection(artifact);

        collection.createIndex(Indexes.compoundIndex(Indexes.ascending("startNode.packageName"), Indexes.ascending("startNode.className"),
                Indexes.ascending("endNode.packageName"), Indexes.ascending("endNode.className")), new IndexOptions().unique(true));

        Pattern excludedPattern = null;
//                Pattern.compile(readExcludedPackages());

        List<WriteModel<Document>> bulkWrites = getAllDocuments(allEdges, excludedPattern, collection);

        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        try {
            collection.bulkWrite(bulkWrites, options);
        } catch (Exception e) {
            System.out.println("duplicate key skipped");
        }

        // 关闭MongoDB客户端连接
//        mongo.close();
    }

    private static List<WriteModel<Document>> getAllDocuments(HashSet<Edge> allEdges, Pattern excludedPattern, MongoCollection<Document> collection) {
        List<WriteModel<Document>> bulkWrites = new ArrayList<>();
        for (Edge edge : allEdges) {
            GraphNode fromNode = edge.getFrom();
            GraphNode toNode = edge.getTo();
//            if (isExcluded(toNode.getPackageName(), excludedPattern)) {
//                continue;
//            }
            Document startNode = new Document("packageName", fromNode.getPackageName())
                    .append("className", fromNode.getClassName())
                    .append("coordinate", fromNode.getCoordinate());

            Document endNode = new Document("packageName", toNode.getPackageName())
                    .append("className", toNode.getClassName())
                    .append("coordinate", toNode.getCoordinate());


            String type = "static";

            Bson filter = Filters.and(Filters.eq("startNode", startNode),
                    Filters.eq("endNode", endNode)
                    , Filters.eq("type", "dynamic"));

            Document existingDocument = collection.find(filter).first();

            if (existingDocument != null) {
                // 已经存在具有相同startNode和endNode，但具有不同type的文档，更新type为both
                Bson update = new Document("$set", new Document("type", "both"));
                bulkWrites.add(new UpdateOneModel<>(filter, update));
            } else {
                // 插入新文档
                Document newDocument = new Document("startNode", startNode)
                        .append("endNode", endNode)
                        .append("type", type);

                bulkWrites.add(new InsertOneModel<>(newDocument));
            }

        }
        return bulkWrites;
    }

    private static boolean isExcluded(String definedPackage, Pattern importPattern) {
        Matcher matcher = importPattern.matcher(definedPackage);
        return matcher.matches();
    }

    private static String readExcludedPackages() {
        StringBuilder str = new StringBuilder();
        try {
            Path path = new File(Constants.EXCLUSION_FILE).toPath();
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
