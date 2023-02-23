package ljystu.project.callgraph.utils;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
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

    public static HashSet<String> findAllCoords() {
        HashSet<String> set = new HashSet<>();
        MongoCollection<Document> collection = mongo.getDatabase("mydatabase").getCollection("mycollection");
        collection.distinct("startNode.coordinate", String.class).into(set);
        return set;


    }
}
