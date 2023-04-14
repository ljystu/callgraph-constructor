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
    public static void uploadEdges(HashSet<Edge> allEdges, String artifact) {
        if (allEdges.isEmpty()) {
            return;
        }

//        artifact = artifact.substring(0, artifact.lastIndexOf(":"));
        MongoDatabase database = mongo.getDatabase("mydatabase");

        MongoCollection<Document> collection = database.getCollection(artifact);

        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("startNode.packageName"), Indexes.ascending("startNode.className")
                , Indexes.ascending("startNode.methodName"), Indexes.ascending("startNode.params"), Indexes.ascending("startNode.returnType")
                , Indexes.ascending("endNode.packageName"), Indexes.ascending("endNode.className")
                , Indexes.ascending("endNode.methodName"), Indexes.ascending("endNode.params"), Indexes.ascending("endNode.returnType")
        ), new IndexOptions().unique(true));

        Pattern excludedPattern = Pattern.compile(readExcludedPackages());

        List<WriteModel<Document>> bulkWrites = getAllDocuments(collection, allEdges, excludedPattern);

        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        try {
            collection.bulkWrite(bulkWrites, options);
        } catch (MongoBulkWriteException e) {
            System.out.println("duplicate key skipped");
        }

    }

    private static List<WriteModel<Document>> getAllDocuments(MongoCollection<Document> collection, HashSet<Edge> allEdges, Pattern excludedPattern) {
        List<WriteModel<Document>> bulkWrites = new ArrayList<>();
        for (Edge edge : allEdges) {
            Node fromNode = edge.getFrom();
            Node toNode = edge.getTo();

            //deprecated
//            if (isExcluded(toNode.getPackageName(), excludedPattern)) {
//                continue;
//            }
            Document startNode = new Document("packageName", fromNode.getPackageName())
                    .append("className", fromNode.getClassName())
                    .append("methodName", fromNode.getMethodName())
                    .append("params", fromNode.getParams())
                    .append("returnType", fromNode.getReturnType())
                    .append("coordinate", fromNode.getCoordinate());

            Document endNode = new Document("packageName", toNode.getPackageName())
                    .append("className", toNode.getClassName())
                    .append("methodName", fromNode.getMethodName())
                    .append("params", fromNode.getParams())
                    .append("returnType", fromNode.getReturnType())
                    .append("coordinate", toNode.getCoordinate());


            Bson filter = Filters.and(
                    Filters.eq("startNode.packageName", startNode.get("packageName")),
                    Filters.eq("endNode.packageName", endNode.get("packageName")),
                    Filters.eq("startNode.className", startNode.get("className")),
                    Filters.eq("endNode.className", endNode.get("className")),
                    Filters.eq("startNode.methodName", startNode.get("methodName")),
                    Filters.eq("endNode.methodName", endNode.get("methodName")),
                    //due to the inconsistency of the params and return type, we do not consider them
//                    Filters.eq("startNode.params", startNode.get("params")),
//                    Filters.eq("endNode.params", endNode.get("params")),
//                    Filters.eq("startNode.returnType", startNode.get("returnType")),
//                    Filters.eq("endNode.returnType", endNode.get("returnType")),
                    Filters.ne("type", "dynamic"));

            Document existingDocument = collection.find(filter).first();

            String type = "dynamic";
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
