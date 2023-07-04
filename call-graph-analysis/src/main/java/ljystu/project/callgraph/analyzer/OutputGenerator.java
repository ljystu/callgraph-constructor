package ljystu.project.callgraph.analyzer;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.MongoEdge;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ljystu
 */
public class OutputGenerator {
    public static HashMap<String, Object> mongoData(String dependencyCoordinate) {
        ServerAddress serverAddress = new ServerAddress(Constants.SERVER_IP_ADDRESS, Constants.MONGO_PORT);
        List<ServerAddress> addrs = new ArrayList<>();
        addrs.add(serverAddress);

        MongoCredential credential = MongoCredential.createScramSha1Credential(Constants.USERNAME, "admin", Constants.MONGO_PASSWORD.toCharArray());
        List<MongoCredential> credentials = new ArrayList<>();
        credentials.add(credential);

        //通过连接认证获取MongoDB连接
        MongoClient mongoClient = new MongoClient(addrs, credentials);

        // 获取MongoDB数据库
        MongoDatabase database = mongoClient.getDatabase("mydatabase");

        // 获取MongoDB集合
        MongoCollection<Document> collection = database.getCollection(dependencyCoordinate);

        HashMap<String, Object> result = new HashMap<>();

        int staticCount = 0;
        int dynamicCount = 0;
        int bothCount = 0;
        int internalDynamicCall = 0;
        int externalDynamicCall = 0;
        int dynCalled = 0;
        int dynCalling = 0;

        HashMap<String, Integer> dynamicCoordinates = new HashMap<>();
        HashMap<String, Integer> staticCoordinates = new HashMap<>();


        HashMap<String, Integer> bothCoordinates = new HashMap<>();

        for (Document document : collection.find()) {
            MongoEdge edge = JSON.parseObject(document.toJson(), MongoEdge.class);

            String endCoordinate = edge.getEndNode().getCoordinate();
            String startCoordinate = edge.getStartNode().getCoordinate();


            if ("static".equals(edge.getType())) {
                staticCount++;
                if (startCoordinate.startsWith(dependencyCoordinate)) {
                    staticCoordinates.put(startCoordinate, staticCoordinates.getOrDefault(startCoordinate, 0) + 1);
                }

            } else if ("dynamic".equals(edge.getType())) {
                if (edge.getStartNode().getMethodName().toLowerCase().contains("test") || edge.getEndNode().getMethodName().toLowerCase().contains("test")
                        || edge.getStartNode().getClassName().toLowerCase().contains("test") || edge.getEndNode().getClassName().toLowerCase().contains("test")) {
                    continue;
                }
                dynamicCount++;
                if (startCoordinate.startsWith(dependencyCoordinate) && endCoordinate.startsWith(dependencyCoordinate)) {
                    internalDynamicCall++;
                    dynamicCoordinates.put(startCoordinate, dynamicCoordinates.getOrDefault(startCoordinate, 0) + 1);
                } else {
                    externalDynamicCall++;
                    if (startCoordinate.startsWith(dependencyCoordinate)) {
                        dynamicCoordinates.put(startCoordinate, dynamicCoordinates.getOrDefault(startCoordinate, 0) + 1);
                        dynCalling++;
                    } else {
                        dynamicCoordinates.put(startCoordinate, dynamicCoordinates.getOrDefault(startCoordinate, 0) + 1);
                        dynCalled++;
                    }
                }
            } else {
                bothCount++;
                if (startCoordinate.startsWith(dependencyCoordinate) || endCoordinate.startsWith(dependencyCoordinate)) {
                    bothCoordinates.put(startCoordinate, bothCoordinates.getOrDefault(startCoordinate, 0) + 1);
                }
            }
        }
        System.out.println("staticCount = " + staticCount);
        System.out.println("dynamicCount = " + dynamicCount);
        System.out.println("internalDynCount = " + internalDynamicCall);
        System.out.println("externalDynCount = " + externalDynamicCall);
        System.out.println("dynCalled = " + dynCalled);
        System.out.println("dynCalling = " + dynCalling);
        System.out.println("bothCount = " + bothCount);

        result.put("staticCount", staticCount);
        result.put("dynamicCount", dynamicCount);
        result.put("internalDynCount", internalDynamicCall);
        result.put("externalDynCount", externalDynamicCall);
        result.put("dynCalled", dynCalled);
        result.put("dynCalling", dynCalling);
        result.put("bothCount", bothCount);


        for (Map.Entry<String, Integer> entry : dynamicCoordinates.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("static");
        for (Map.Entry<String, Integer> entry : staticCoordinates.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("both");
        for (Map.Entry<String, Integer> entry : bothCoordinates.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        result.put("dynamicCoordinates", dynamicCoordinates);
        result.put("staticCoordinates", staticCoordinates);
        result.put("bothCoordinates", bothCoordinates);
        // close mongo client
        mongoClient.close();
        return result;
    }

    public static void outputToJson(HashMap<String, HashMap<String, Object>> analysisResult, File file) {
        System.out.println("output to json: " + file.getAbsolutePath());
        if (!file.exists()) {
            try {
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        //analysis result to json
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(file, analysisResult);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
