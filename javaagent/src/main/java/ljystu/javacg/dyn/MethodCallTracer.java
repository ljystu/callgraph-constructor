package ljystu.javacg.dyn;

import com.alibaba.fastjson.JSON;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import redis.clients.jedis.Jedis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class MethodCallTracer {

    public static Jedis jedis;
    public static final Object logFileLock = new Object();

    public static String redisKey;
    public static String packagePrefix;
    public static final ThreadLocal<Deque<Node>> callStack = ThreadLocal.withInitial(ArrayDeque::new);

    public static Set<Edge> set = Collections.synchronizedSet(new HashSet<>());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {

                synchronized (logFileLock) {
//                jedis.close();

                    try {
                        System.out.println("write start");
                        String logPath =
//                            "/scratch/jingyuli/repos/analyzed_logs/";
                                "/Users/ljystu/Desktop/neo4j/call-graph-analysis/logs/";
                        String filePath = logPath + redisKey + ".log";
                        Path path = Paths.get(filePath);
                        if (!Files.exists(path)) {
                            Files.createFile(path);
                        }

                        BufferedWriter writer = new BufferedWriter(new FileWriter(
                                logPath + redisKey + ".log", true));

                        for (Edge edge : set) {
//                            System.out.println(JSON.toJSONString(edge));
                            writer.write(JSON.toJSONString(edge));
                            writer.newLine();
                        }
                        System.out.println("write over");
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

//        jedis = new Jedis("34.30.61.85");
//        jedis.auth("ljystu");


    }

    public static void premain(String args, Instrumentation instrumentation) throws ClassNotFoundException {
        try {
            // 请根据实际情况更改 gsonJarPath
            String gsonJarPath =
                    "/Users/ljystu/Downloads/gson-2.10.jar";
//                    "/scratch/jingyuli/repos/project_files/gson-2.10.jar";
            File gsonJarFile = new File(gsonJarPath);
            URL gsonJarURL = gsonJarFile.toURI().toURL();

            // 创建一个新的 URLClassLoader 并将 gsonJarURL 添加到类路径中
            URLClassLoader customClassLoader = new URLClassLoader(new URL[]{gsonJarURL});

            // 使用自定义类加载器加载 ReflectionHelper 类
            Class<?> reflectionHelperClass = customClassLoader.loadClass("com.google.gson.internal.reflect.ReflectionHelper");

            // 如果需要，您可以使用 reflectionHelperClass 创建实例或调用方法
            System.out.println("Loaded ReflectionHelper class: " + reflectionHelperClass);
        } catch (Exception e) {
            e.printStackTrace();
        }


        String[] split = args.split("!");
        redisKey = split[1];
        packagePrefix = split[0];
        String[] prefixes = packagePrefix.split(",");

        ElementMatcher.Junction typeMatcher = null;
        for (String prefix : prefixes) {
            if (typeMatcher == null) {
                typeMatcher = ElementMatchers.nameStartsWith(prefix.trim());
            } else {
                typeMatcher = typeMatcher.or(ElementMatchers.nameStartsWith(prefix.trim()));
            }
        }

        typeMatcher = typeMatcher.and(ElementMatchers.not(ElementMatchers.nameMatches(".*(surefire|junit|testng|mockito|powermock|easymock|spockframework|hamcrest).*")))
                .and(ElementMatchers.not(ElementMatchers.nameContainsIgnoreCase("test")));

        packagePrefix = prefixes[prefixes.length - 1];

        ByteBuddyAgent.install();

        AgentBuilder agentBuilder1 = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.DISABLED)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
//                .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module) ->
                        builder.visit(Advice.to(MethodCallTracingAdvice.class).on(ElementMatchers.not(ElementMatchers.isConstructor()))
                        )
                );

        AgentBuilder agentBuilder2 = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.DISABLED)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
//                .with(AgentBuilder.Listener.StreamWriting.toSystemError())
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module) ->
                        builder.visit(Advice.to(ConstructorCallTrace.class).on(ElementMatchers.isConstructor()))
                );

        agentBuilder1.installOn(instrumentation);
//        agentBuilder2.installOn(instrumentation);


    }

    public static class MethodCallTracingAdvice {

        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Origin String origin,
                                         @Advice.Origin("#t") String className,
                                         @Advice.Origin("#m") String methodName,
                                         @Advice.Origin("#s") String methodSignature,
                                         @Advice.Origin("#r") String returnType) {
            handleMethodEnter(origin, className, methodName, methodSignature, returnType);
        }


        public static void handleMethodEnter(String origin, String className, String methodName, String methodSignature, String returnType) {
//            System.out.println(origin);
            String packageName = className.substring(0, className.lastIndexOf('.'));
            String classNameWithoutPackage = className.substring(className.lastIndexOf('.') + 1);
            String params = methodSignature;

            params = params.substring(params.indexOf("(") + 1, params.indexOf(")"));

            String accessModifier = origin.substring(0, origin.indexOf(' '));
            Node callee = new Node(packageName, classNameWithoutPackage, methodName, params, returnType, origin, accessModifier);

            Deque<Node> stack = callStack.get();
            Node caller = !stack.isEmpty() ? stack.peek() : null;
            stack.push(callee);

            if (caller != null) {
                if (caller.getPackageName().startsWith(packagePrefix) || callee.getPackageName().startsWith(packagePrefix)) {
                    Edge edge = new Edge(caller, callee);
                    set.add(edge);
                }
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onMethodExit(@Advice.Origin String origin) {
            Deque<Node> stack = callStack.get();

            if (!stack.isEmpty() && stack.peek().getOrigin().equals(origin)) {
                stack.pop();
            }
        }
    }

    public static class ConstructorCallTrace {
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Origin String origin,
                                         @Advice.Origin("#t") String className,
                                         @Advice.Origin("#m") String methodName,
                                         @Advice.Origin("#s") String methodSignature) {
            handleMethodEnter(origin, className, methodName, methodSignature, "void");
        }

        public static void handleMethodEnter(String origin, String className, String methodName, String methodSignature, String returnType) {
            System.out.println(origin);
            String packageName = className.substring(0, className.lastIndexOf('.'));
            String classNameWithoutPackage = className.substring(className.lastIndexOf('.') + 1);
            String params = methodSignature;

            params = params.substring(params.indexOf("(") + 1, params.indexOf(")"));

            String accessModifier = origin.substring(0, origin.indexOf(' '));
            Node callee = new Node(packageName, classNameWithoutPackage, methodName, params, returnType, origin, accessModifier);

            Deque<Node> stack = callStack.get();
            Node caller = !stack.isEmpty() ? stack.peek() : null;
            stack.push(callee);

            if (caller != null) {
                if (caller.getPackageName().startsWith(packagePrefix) || callee.getPackageName().startsWith(packagePrefix)) {
                    Edge edge = new Edge(caller, callee);
                    set.add(edge);
                }
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onMethodExit(@Advice.Origin String origin) {
            Deque<Node> stack = callStack.get();

            if (!stack.isEmpty() && stack.peek().getOrigin().equals(origin)) {
                stack.pop();
            }
        }
    }


//    public static class MethodCallTracingAdvice {
//
//        @Advice.OnMethodEnter
//        public static void onMethodEnter(@Advice.Origin String origin,
//                                         @Advice.Origin("#t") String className,
//                                         @Advice.Origin("#m") String methodName,
//                                         @Advice.Origin("#s") String methodSignature,
//                                         @Advice.Origin("#r") String returnType) {
//
//            System.out.println(origin);
//            String packageName = className.substring(0, className.lastIndexOf('.'));
//            String classNameWithoutPackage = className.substring(className.lastIndexOf('.') + 1);
//            String params = methodSignature;
//
//            params = params.substring(params.indexOf("(") + 1, params.indexOf(")"));
//
//            String accessModifier = origin.substring(0, origin.indexOf(' '));
//            Node callee = new Node(packageName, classNameWithoutPackage, methodName, params, returnType, origin, accessModifier);
//
//            Deque<Node> stack = callStack.get();
//            Node caller = !stack.isEmpty() ? stack.peek() : null;
//            stack.push(callee);
//
//            if (caller != null) {
//                if (caller.getPackageName().startsWith(packagePrefix) || callee.getPackageName().startsWith(packagePrefix)) {
//                    Edge edge = new Edge(caller, callee);
//                    set.add(edge);
//                }
//            }
//        }
//
//
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        public static void onMethodExit(@Advice.Origin String origin) {
//            Deque<Node> stack = callStack.get();
//
////            if (throwable != null) {
////                System.err.println("Error: Exception occurred in method " + origin + ": " + throwable.getMessage());
////            }
//
//            if (!stack.isEmpty() && stack.peek().getOrigin().equals(origin)) {
//                stack.pop();
//            }
////            else {
////                System.err.println("Error: Mismatched method exit for " + origin);
////            }
//        }
//
//
//    }
}
