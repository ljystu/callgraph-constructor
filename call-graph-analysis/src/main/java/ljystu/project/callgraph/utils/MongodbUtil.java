package ljystu.project.callgraph.utils;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
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

    public static void deleteEdges(String coord) {
        // 创建MongoDB客户端连接

        // 获取MongoDB数据库

        MongoDatabase database = mongo.getDatabase("mydatabase");

        // 获取MongoDB集合

        MongoCollection<Document> collection = database.getCollection("mycollection");

        try {
            Bson filter = Filters.eq("startNode.coordinate", coord);
            collection.deleteMany(filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 关闭MongoDB客户端连接
//        mongo.close();
    }

    /**
     * Upload edges.
     *
     * @param allEdges the all edges
     */
    public static void uploadEdges(HashSet<Edge> allEdges, String artifact) {
        if (allEdges.isEmpty()) {
            return;
        }

        artifact = artifact.substring(0, artifact.lastIndexOf(":"));
        MongoDatabase database = mongo.getDatabase("mydatabase");

        MongoCollection<Document> collection = database.getCollection(artifact);

        collection.createIndex(Indexes.compoundIndex(Indexes.ascending("startNode.packageName"), Indexes.ascending("startNode.className"),
                Indexes.ascending("startNode.coordinate"), Indexes.ascending("endNode.packageName"), Indexes.ascending("endNode.className"),
                Indexes.ascending("endNode.coordinate"), Indexes.ascending("type")), new IndexOptions().unique(true));

        Pattern excludedPattern = Pattern.compile(readExcludedPackages());

        List<WriteModel<Document>> bulkWrites = getAllDocuments(allEdges, excludedPattern);

        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        try {
            collection.bulkWrite(bulkWrites, options);
        } catch (MongoBulkWriteException e) {
            System.out.println("duplicate key skipped");
        }

        // 关闭MongoDB客户端连接
//        mongo.close();
    }

    private static List<WriteModel<Document>> getAllDocuments(HashSet<Edge> allEdges, Pattern excludedPattern) {
        List<WriteModel<Document>> bulkWrites = new ArrayList<>();
        for (Edge edge : allEdges) {
            Node fromNode = edge.getFrom();
            Node toNode = edge.getTo();
//            if (isExcluded(toNode.getPackageName(), excludedPattern)) {
//                continue;
//            }
            Document startNode = new Document("packageName", fromNode.getPackageName())
                    .append("className", fromNode.getClassName())
                    .append("coordinate", fromNode.getCoordinate());

            Document endNode = new Document("packageName", toNode.getPackageName())
                    .append("className", toNode.getClassName())
                    .append("coordinate", toNode.getCoordinate());

            Document mongoEdge = new Document("startNode", startNode)
                    .append("endNode", endNode).append("type", "dynamic");

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


    public static HashSet<String> findAllCoords() {
        HashSet<String> set = new HashSet<>();
        MongoCollection<Document> collection = mongo.getDatabase("mydatabase").getCollection("mycollection");
        collection.distinct("startNode.coordinate", String.class).into(set);
        return set;


    }
}
