package ljystu.project.callgraph.uploader;

import com.alibaba.fastjson.JSON;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Edge;
import ljystu.project.callgraph.entity.Node;
import ljystu.project.callgraph.utils.MongodbUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static ljystu.project.callgraph.utils.PackageUtil.packageToCoordMap;

/**
 * The type Redis op.
 *
 * @author ljystu
 */
@Slf4j
public class CallGraphUploader {

    /**
     * The Jedis.
     */
    Jedis jedis;
    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    JedisPool jedisPool;

    // 设置连接超时和读取超时
    int connectionTimeout = 2000; // 2 seconds
    int readTimeout = 2000; // 2 seconds


    /**
     * The Neo 4 j utils.
     */
    Neo4jOp neo4JOp;

    /**
     * Instantiates a new Redis op.
     */
    public CallGraphUploader() {
        jedisPoolConfig.setMaxTotal(50);
        jedisPoolConfig.setMaxIdle(0);
//        jedisPoolConfig.setMinIdle(5);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        jedisPoolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        jedisPoolConfig.setNumTestsPerEvictionRun(3);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPoolConfig.setJmxEnabled(true);
        jedisPoolConfig.setJmxNamePrefix("jedis-pool");
        jedisPool = new JedisPool(jedisPoolConfig, Constants.SERVER_IP_ADDRESS, 6379, connectionTimeout, "ljystu");

//        this.neo4JOp = new Neo4jOp(Constants.NEO4J_PORT, Constants.NEO4J_USERNAME, Constants.NEO4J_PASSWORD);
//        this.jedis = new Jedis(Constants.SERVER_IP_ADDRESS);
//        this.jedis.auth(Constants.REDIS_PASSWORD);
    }

    public void uploadAll(String dependencyCoordinate, String artifactId) {

        uploadFromFile(artifactId, packageToCoordMap, dependencyCoordinate);

//        jedis.close();
        jedisPool.close();
    }

    public void copyPackageToCoordMap() {

        jedisPool.getResource().keys("packageToCoordMap").forEach(key -> {
            jedisPool.getResource().del(key);
        });

    }


    /**
     * Upload.
     *
     * @param label             the label
     * @param packageToCoordMap the map
     */
    @Deprecated
    private void upload(String label, Map<String, String> packageToCoordMap, String dependencyCoordinate) {

//        HashSet<Node> nodes = new HashSet<>();
        HashSet<Edge> edges = new HashSet<>();

        // 遍历所有键，获取对应的值并删除
        for (String value : jedis.smembers(label)) {

            Edge edge;
            try {
                edge = JSON.parseObject(value, Edge.class);
            } catch (Exception e) {
                continue;
            }
            if (edge == null) {
                continue;
            }

            Node nodeFrom = edge.getFrom();
            Node nodeTo = edge.getTo();

            getFullCoordinates(nodeFrom, packageToCoordMap);
            getFullCoordinates(nodeTo, packageToCoordMap);

            if (Objects.equals(nodeFrom.getCoordinate(), null)) {
                nodeFrom.setCoordinate("not found");
            }
            if (Objects.equals(nodeTo.getCoordinate(), null)) {
                nodeTo.setCoordinate("not found");
            }

            if (edge.getFrom().getPackageName().startsWith(Constants.PACKAGE_PREFIX) ||
                    edge.getTo().getPackageName().startsWith(Constants.PACKAGE_PREFIX)) {
                Edge newEdge = new Edge(nodeFrom, nodeTo);
                log.info("Edge upload: " + newEdge);
//                System.out.println("Edge upload: " + newEdge);

//                nodes.add(nodeFrom);
//                nodes.add(nodeTo);

                edges.add(newEdge);
            }

        }

//        List<Node> nodesList = new ArrayList<>(nodes);
//        neo4JOp.uploadAllToNeo4j(nodesList, edges, label);
        MongodbUtil.uploadEdges(edges, dependencyCoordinate);


    }


    /**
     * Upload from file.
     *
     * @param label             the label
     * @param packageToCoordMap the map
     */
    private void uploadFromFile(String label, Map<String, String> packageToCoordMap, String dependencyCoordinate) {

        HashSet<Edge> edges = new HashSet<>();
        Map<String, String> redisMap = jedisPool.getResource().hgetAll(dependencyCoordinate);
        for (Map.Entry<String, String> entry : redisMap.entrySet()) {
            if (!packageToCoordMap.containsKey(entry.getKey())) {
                packageToCoordMap.put(entry.getKey(), entry.getValue());
            }
        }
//        packageToCoordMap.putAll(redisMap);
        String filePath = dependencyCoordinate + ".log";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {

                Edge edge;
                try {
                    edge = JSON.parseObject(line, Edge.class);
                } catch (Exception e) {
                    continue;
                }
                if (edge == null || edges.contains(edge)) {
                    continue;
                }

                Node nodeFrom = edge.getFrom();
                Node nodeTo = edge.getTo();

                nodeTypeTransform(nodeFrom);
                nodeTypeTransform(nodeTo);

                getFullCoordinates(nodeFrom, packageToCoordMap);
                getFullCoordinates(nodeTo, packageToCoordMap);

                if (Objects.equals(nodeFrom.getCoordinate(), null)) {
                    nodeFrom.setCoordinate("not found");
                }
                if (Objects.equals(nodeTo.getCoordinate(), null)) {
                    nodeTo.setCoordinate("not found");
                }

                if (edge.getFrom().getPackageName().startsWith(Constants.PACKAGE_PREFIX) ||
                        edge.getTo().getPackageName().startsWith(Constants.PACKAGE_PREFIX)) {
                    Edge newEdge = new Edge(nodeFrom, nodeTo);
                    log.info("Edge upload: " + newEdge);
                    edges.add(newEdge);
                }

            }
            System.out.println("edges size: " + edges.size());
            clearFile(filePath);
            MongodbUtil.uploadEdges(edges, dependencyCoordinate);


        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
//            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearFile(String filename) {
        try {
            FileWriter fw = new FileWriter(filename, false);
            fw.write("");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFullCoordinates(Node nodeFrom, Map<String, String> packageToCoordMap) {
        Pattern pattern = Pattern.compile("^(java|javax|jdk|sun|com\\.sun|org\\.w3c|org\\.xml|org\\.ietf|org\\.omg|org\\.jcp).*");
        if (pattern.matcher(nodeFrom.getPackageName()).matches()) {
            nodeFrom.setCoordinate("java_rt");
            return;
        }

        String nodeFromMavenCoord = packageToCoordMap.get(nodeFrom.getPackageName());

        nodeFrom.setCoordinate(nodeFromMavenCoord);

    }


    public static void nodeTypeTransform(Node node) {
        String[] param = node.getParams().split(",");
        StringBuilder str = new StringBuilder();
        if (param.length > 0) {
            for (int i = 0; i < param.length; i++) {
                if (param[i].length() == 0) break;
                String paramType = param[i];
//                        .substring(1);
                paramType = typeTransform(paramType);
                str.append(paramType).append(",");
            }
            if (str.length() > 0)
                str.setLength(str.length() - 1);
        }
        node.setParams(str.toString());
        node.setReturnType(typeTransform(node.getReturnType()));
    }

    public static String typeTransform(String type) {
        if (type.startsWith("[")) {
            int arrayDimension = 0;
            while (type.startsWith("[")) {
                arrayDimension++;
                type = type.substring(1);
            }

            String fullQualifiedType;
            switch (type) {
                case "B":
                    fullQualifiedType = "byte";
                    break;
                case "C":
                    fullQualifiedType = "char";
                    break;
                case "D":
                    fullQualifiedType = "double";
                    break;
                case "F":
                    fullQualifiedType = "float";
                    break;
                case "I":
                    fullQualifiedType = "int";
                    break;
                case "J":
                    fullQualifiedType = "long";
                    break;
                case "S":
                    fullQualifiedType = "short";
                    break;
                case "Z":
                    fullQualifiedType = "boolean";
                    break;
                default:
                    // Remove 'L' and ';'
                    fullQualifiedType = type.substring(1, type.length() - 1);
            }

            for (int i = 0; i < arrayDimension; i++) {
                fullQualifiedType += "[]";
            }
            return fullQualifiedType;
        }

        switch (type) {
            case "java.lang.BooleanType":
                return "boolean";
            case "java.lang.VoidType":
                return "void";
            case "java.lang.IntegerType":
                return "int";
            case "java.lang.LongType":
                return "long";
            case "java.lang.FloatType":
                return "float";
            case "java.lang.DoubleType":
                return "double";
            case "java.lang.ByteType":
                return "byte";
            case "java.lang.ShortType":
                return "short";
            case "java.lang.CharacterType":
                return "char";
            case "java.lang.BooleanType[]":
                return "boolean[]";
            case "java.lang.VoidType[]":
                return "void[]";
            case "java.lang.IntegerType[]":
                return "int[]";
            case "java.lang.LongType[]":
                return "long[]";
            case "java.lang.FloatType[]":
                return "float[]";
            case "java.lang.DoubleType[]":
                return "double[]";
            case "java.lang.ByteType[]":
                return "byte[]";
            case "java.lang.ShortType[]":
                return "short[]";
            case "java.lang.CharacterType[]":
                return "char[]";
            default:
                return type;
        }
    }
}