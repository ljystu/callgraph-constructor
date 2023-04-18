package ljystu.project.callgraph.utils;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author ljystu
 */
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
//            properties.load(MongodbUtil.class.getClassLoader().getResourceAsStream("mongo.properties"));
            host =
                    Constants.SERVER_IP_ADDRESS;
//                    properties.getProperty("host");
            port = Constants.MONGO_PORT;
//                    Integer.parseInt(properties.getProperty("port"));
            poolSize = 10;
//                    Integer.parseInt(properties.getProperty("poolSize"));
            blockSize = 10;
//                    Integer.parseInt(properties.getProperty("blockSize"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // 设置连接参数
            ServerAddress serverAddress = new ServerAddress(host, port);
            MongoCredential credential = MongoCredential.createScramSha1Credential(Constants.USERNAME, "admin", Constants.MONGO_PASSWORD.toCharArray());
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
    public static void uploadEdges(HashSet<Edge> allEdges, String dependencyWithoutVersion) {
        if (allEdges.isEmpty()) {
            return;
        }

        MongoDatabase database = mongo.getDatabase("mydatabase");

        MongoCollection<Document> collection = database.getCollection(dependencyWithoutVersion);

        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("startNode.packageName"), Indexes.ascending("startNode.className")
                , Indexes.ascending("startNode.methodName"), Indexes.ascending("startNode.params"), Indexes.ascending("startNode.returnType")
                , Indexes.ascending("endNode.packageName"), Indexes.ascending("endNode.className")
                , Indexes.ascending("endNode.methodName"), Indexes.ascending("endNode.params"), Indexes.ascending("endNode.returnType")
        ), new IndexOptions().unique(true));

        Pattern excludedPattern = Pattern.compile(readExcludedPackages());

        Map<String, Document> existingDocumentsMap = queryExistingDocuments(collection);

        List<WriteModel<Document>> bulkWrites = new ArrayList<>(getAllDocuments(allEdges, excludedPattern, collection, existingDocumentsMap));

        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        try {
            collection.bulkWrite(bulkWrites, options);
        } catch (MongoBulkWriteException e) {
            System.out.println("duplicate key skipped");
        }

    }

    private static HashSet<WriteModel<Document>> getAllDocuments(HashSet<Edge> allEdges, Pattern excludedPattern, MongoCollection<Document> collection, Map<String, Document> existingDocumentsMap) {
        HashSet<WriteModel<Document>> bulkWrites = new HashSet<>();
        for (Edge edge : allEdges) {
            Node fromNode = edge.getFrom();
            Node toNode = edge.getTo();
//            if (isExcluded(toNode.getPackageName(), excludedPattern)) {
//                continue;
//            }

            Document startNode = new Document("packageName", fromNode.getPackageName())
                    .append("className", fromNode.getClassName())
                    .append("methodName", fromNode.getMethodName());

            Document endNode = new Document("packageName", toNode.getPackageName())
                    .append("className", toNode.getClassName())
                    .append("methodName", toNode.getMethodName());
            endNode.append("params", toNode.getParams())
                    .append("returnType", toNode.getReturnType())
                    .append("coordinate", toNode.getCoordinate());
            startNode.append("params", fromNode.getParams())
                    .append("returnType", fromNode.getReturnType())
                    .append("coordinate", fromNode.getCoordinate());


//            Bson filter = Filters.and(
//                    Filters.eq("startNode.packageName", startNode.get("packageName")),
//                    Filters.eq("endNode.packageName", endNode.get("packageName")),
//                    Filters.eq("startNode.className", startNode.get("className")),
//                    Filters.eq("endNode.className", endNode.get("className")),
//                    Filters.eq("startNode.methodName", startNode.get("methodName")),
//                    Filters.eq("endNode.methodName", endNode.get("methodName")),
////                    Filters.eq("startNode.params", startNode.get("params")),
////                    Filters.eq("endNode.params", endNode.get("params")),
////                    Filters.eq("startNode.returnType", startNode.get("returnType")),
////                    Filters.eq("endNode.returnType", endNode.get("returnType")),
//                    Filters.ne("type", "static"));
//
//            filters.add(filter);

            Document existingDocument = existingDocumentsMap.get(generateKey(startNode, endNode));

            String type = "static";
            if (existingDocument != null) {
                // 已经存在具有相同startNode和endNode，但具有不同type的文档，更新type为both
                Bson filter = Filters.eq("_id", existingDocument.getObjectId("_id"));

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

    private static String generateKey(Document startNode, Document endNode) {
        return startNode.get("packageName") + "-" +
                startNode.get("className") + "-" +
                startNode.get("methodName") + "-" +
                endNode.get("packageName") + "-" +
                endNode.get("className") + "-" +
                endNode.get("methodName");
    }

    private static Map<String, Document> queryExistingDocuments(MongoCollection<Document> collection) {
        int pageSize = 1000;
        int currentPage = 0;
        Bson filter = Filters.ne("type", "static");
        Map<String, Document> existingDocumentsMap = new HashMap<>();

        long totalCount = collection.countDocuments(filter);

        while (currentPage * pageSize < totalCount) {
            FindIterable<Document> existingDocuments = collection.find(filter)
                    .skip(currentPage * pageSize)
                    .limit(pageSize);

            for (Document existingDocument : existingDocuments) {
                Document startNode = existingDocument.get("startNode", Document.class);
                Document endNode = existingDocument.get("endNode", Document.class);
                String key = generateKey(startNode, endNode);
                existingDocumentsMap.put(key, existingDocument);
            }

            currentPage++;
        }

        return existingDocumentsMap;
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
