package ljystu.project.callgraph;


import ljystu.project.callgraph.config.Path;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloExampleTest {
    static org.apache.maven.shared.invoker.Invoker mavenInvoker = new DefaultInvoker();

    static String mavenPath = Path.getMavenHome();
    static String jarPath = Path.getJavaagentHome();

    @Test
    public void testAll() throws Exception {
//        Driver driver = GraphDatabase.driver("bolt://localhost:7687",
//                AuthTokens.basic("neo4j", "ljystu"));
//        Session session = driver.session();
//
//        final String message = "Greeting";
//        String greeting = session.writeTransaction(
//                new TransactionWork<String>() {
//                    public String execute(Transaction tx) {
//                        Result result = tx.run("CREATE (a:Greeting) " +
//                                        "SET a.message = $message " +
//                                        "RETURN a.message + ', from node ' + id(a)",
//                                parameters("message", message));
//                        return result.single().get(0).asString();
//                    }
//                }
//        );

//        System.out.println(greeting);

//        System.out.println("OK");
////        driver.close();
//        Neo4j neo4jUtil = new Neo4j();
//        neo4jUtil = new Neo4j();
//        neo4jUtil.close();
//        System.out.println("bolt");
//        System.out.println;


//        File directory= new File("zookeeper-master").getAbsoluteFile();
//        deleteFile(directory);

//        File dir = new File("/path/to/dir");
//        URL url = dir.toURI().toURL();
//        URL[] urls = new URL[]{url};
//
//        for (URL url1: urls){
//            System.out.println(url1.toString());
//        }

    }

    @Test
    public void mavenInfoTest() throws IOException, XmlPullParserException, MavenInvocationException {

        mavenInvoker.setMavenHome(new File(mavenPath));

        // 创建InvocationRequest
        InvocationRequest request = new DefaultInvocationRequest();

        String rootPath = "/Users/ljystu/Desktop/neo4j";
        // 设置项目的路径
//        String projectMavenFilePath = rootPath + "/pom.xml";
//        File projectMavenFile = new File(projectMavenFilePath);
//
//        request.setPomFile(projectMavenFile);
//
//
//        request.setGoals(Arrays.asList("dependency:list"));
//        Properties properties = new Properties();
//        properties.setProperty("outputFile", "dependencies.txt"); // redirect output to a file
//        properties.setProperty("outputAbsoluteArtifactFilename", "true"); // with paths
//        properties.setProperty("includeScope", "runtime"); // only runtime (scope compile + runtime)
//// if only interested in scope runtime, you may replace with excludeScope = compile
//        request.setProperties(properties);
//
//        mavenInvoker.execute(request);

        String s = execCmd("mvn dependency:list", rootPath);
        String[] split = s.split("\n");
        HashSet<String> dependencies = new HashSet<>();
        Pattern pattern = Pattern.compile("    (.*):compile|runtime");

        for (String line : split) {
            if (line == null) continue;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                // group 1 contains the path to the file

                String match = matcher.group(1);
                if (match == null) continue;
                String[] info = match.split(":");
                // TODO summarize ignore packages
                if (info[0].startsWith("java")) continue;

                String mavenAddress = info[0] + ":" + info[1] + ":" + info[3];
                dependencies.add(mavenAddress);

            }
        }


        System.out.println(dependencies.size());
    }

    public static String execCmd(String cmd, String dir) {
        String result = null;
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd, null, new File(dir)).getInputStream();
             Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


}

