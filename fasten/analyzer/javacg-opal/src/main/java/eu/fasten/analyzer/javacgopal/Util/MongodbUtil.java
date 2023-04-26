package eu.fasten.analyzer.javacgopal.Util;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The type Mongodb util.
 */
public class MongodbUtil {

    private static MongoClient mongo = null;

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
            ServerAddress serverAddress = new ServerAddress(Constants.MONGO_ADDRESS, Constants.MONGO_PORT);
            // 创建连接对象
            MongoCredential credential = MongoCredential.createScramSha1Credential(Constants.username, "admin", Constants.password.toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            credentials.add(credential);
            mongo = new MongoClient(serverAddress, credentials);
        }
        if (allEdges.isEmpty()) {
            return;
        }

        artifact = artifact.substring(0, artifact.lastIndexOf(":"));
        MongoDatabase database = mongo.getDatabase("mydatabase");

        MongoCollection<Document> collection = database.getCollection(artifact);

        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("startNode.packageName"), Indexes.ascending("startNode.className")
                , Indexes.ascending("startNode.methodName"), Indexes.ascending("startNode.params"), Indexes.ascending("startNode.returnType")
                , Indexes.ascending("endNode.packageName"), Indexes.ascending("endNode.className")
                , Indexes.ascending("endNode.methodName"), Indexes.ascending("endNode.params"), Indexes.ascending("endNode.returnType")
        ), new IndexOptions().unique(true));

        Pattern excludedPattern = null;
//                Pattern.compile(readExcludedPackages());
        Map<String, Document> existingDocumentsMap = queryExistingDocuments(collection);

        List<WriteModel<Document>> bulkWrites = new ArrayList<>(getAllDocuments(allEdges, excludedPattern, collection, existingDocumentsMap));

        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        try {
            collection.bulkWrite(bulkWrites, options);
        } catch (Exception e) {
            System.out.println("duplicate key skipped");
        }

        // 关闭MongoDB客户端连接
//        mongo.close();
    }

    private static HashSet<WriteModel<Document>> getAllDocuments(HashSet<Edge> allEdges, Pattern excludedPattern, MongoCollection<Document> collection, Map<String, Document> existingDocumentsMap) {
        HashSet<WriteModel<Document>> bulkWrites = new HashSet<>();
        for (Edge edge : allEdges) {
            GraphNode fromNode = edge.getFrom();
            GraphNode toNode = edge.getTo();
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
