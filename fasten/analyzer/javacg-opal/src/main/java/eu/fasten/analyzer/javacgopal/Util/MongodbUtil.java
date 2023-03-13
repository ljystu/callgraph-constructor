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
import eu.fasten.analyzer.javacgopal.Constants;
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
    public static void uploadEdges(HashSet<Edge> allEdges) {
        if (allEdges.isEmpty()) return;

        MongoDatabase database = mongo.getDatabase("mydatabase");
        if (edgeCount > 50000000) {
            collectionCount++;
            edgeCount = 0;
        }

        MongoCollection<Document> collection = database.getCollection("mycollection" + collectionCount);

        List<WriteModel<Document>> bulkWrites = new ArrayList<>();

        Pattern excludedPattern = Pattern.compile(readExcludedPackages());

        edgeCount += allEdges.size();

        addDocuments(allEdges, bulkWrites, excludedPattern);

        try {
            BulkWriteResult result = collection.bulkWrite(bulkWrites);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 关闭MongoDB客户端连接
//        mongo.close();
    }

    private static void addDocuments(HashSet<Edge> allEdges, List<WriteModel<Document>> bulkWrites, Pattern excludedPattern) {
        for (Edge edge : allEdges) {
            GraphNode fromNode = edge.getFrom();
            GraphNode toNode = edge.getTo();
            if (isExcluded(toNode.getPackageName(), excludedPattern)) {
                continue;
            }
            Document startNode = new Document("packageName", fromNode.getPackageName())
                    .append("className", fromNode.getClassName())
                    .append("coordinate", fromNode.getCoordinate());


            Document endNode = new Document("packageName", toNode.getPackageName())
                    .append("className", toNode.getClassName())
                    .append("coordinate", toNode.getCoordinate());

            Document mongoEdge = new Document("startNode", startNode)
                    .append("endNode", endNode).append("type", "static");

            //upsert
//            Bson filter = Filters.and(
//                    Filters.eq("startNode.packageName", fromNode.getPackageName()),
//                    Filters.eq("startNode.className", fromNode.getClassName()),
//                    Filters.eq("startNode.coordinate", fromNode.getCoordinate()),
//                    Filters.eq("endNode.packageName", toNode.getPackageName()),
//                    Filters.eq("endNode.className", toNode.getClassName()),
//                    Filters.eq("endNode.coordinate", toNode.getCoordinate())
//            );
//            Bson update = Updates.combine(
//                    Updates.setOnInsert("startNode", startNode),
//                    Updates.setOnInsert("endNode", endNode),
//                    Updates.set("type", "static")
////                    Updates.currentDate("lastModified")
//            );
//            bulkWrites.add(new UpdateOneModel<Document>(filter, update, new UpdateOptions().upsert(true)));
            bulkWrites.add(new InsertOneModel<>(mongoEdge));

        }
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
