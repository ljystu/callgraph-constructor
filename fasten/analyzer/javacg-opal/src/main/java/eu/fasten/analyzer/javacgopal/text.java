package eu.fasten.analyzer.javacgopal;

import eu.fasten.analyzer.javacgopal.entity.Edge;
import eu.fasten.analyzer.javacgopal.entity.GraphNode;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class text {
    public static void writeNodes(HashMap<Integer, GraphNode> nodes, HashSet<Edge> edges) {
        //第一步：设置输出的文件路径
        //如果该目录下不存在该文件，则文件会被创建到指定目录下。如果该目录有同名文件，那么该文件将被覆盖。
        File writeFile = new File("src/main/resources/writeNode.csv");

        try {
            //第二步：通过BufferedReader类创建一个使用默认大小输出缓冲区的缓冲字符输出流
            BufferedWriter writeText = new BufferedWriter(new FileWriter(writeFile));

            for (Map.Entry<Integer, GraphNode> entry : nodes.entrySet()) {
                GraphNode value = entry.getValue();
                writeText.write(value.getPackageName() + "," + value.getClassName() + "," + value.getMethodName() + "," +
                        value.getParams() + "," + value.getReturnType());
            }
            //第三步：将文档的下一行数据赋值给lineData，并判断是否为空，若不为空则输出


            //使用缓冲区的刷新方法将数据刷到目的地中
            writeText.flush();
            //关闭缓冲区，缓冲区没有调用系统底层资源，真正调用底层资源的是FileWriter对象，缓冲区仅仅是一个提高效率的作用
            //因此，此处的close()方法关闭的是被缓存的流对象
            writeText.close();
        } catch (FileNotFoundException e) {
            System.out.println("没有找到指定文件");
        } catch (IOException e) {
            System.out.println("文件读写出错");
        }
    }

//    public static void writeEdges(HashMap<Integer, GraphNode> nodes, HashSet<Edge> edges) {
//        //第一步：设置输出的文件路径
//        //如果该目录下不存在该文件，则文件会被创建到指定目录下。如果该目录有同名文件，那么该文件将被覆盖。
//        File writeFile = new File("src/main/resources/writeEdge.csv");
//
//        try {
//            //第二步：通过BufferedReader类创建一个使用默认大小输出缓冲区的缓冲字符输出流
//            BufferedWriter writeText = new BufferedWriter(new FileWriter(writeFile));
//
//            for (Map.Entry<Integer, GraphNode> entry : nodes.entrySet()) {
//                GraphNode value = entry.getValue();
//                writeText.write(value.getPackageName() + "," + value.getClassName() + "," + value.getMethodName() + "," +
//                        value.getParams() + "," + value.getReturnType());
//            }
//            //第三步：将文档的下一行数据赋值给lineData，并判断是否为空，若不为空则输出
//
//            //使用缓冲区的刷新方法将数据刷到目的地中
//            writeText.flush();
//            //关闭缓冲区，缓冲区没有调用系统底层资源，真正调用底层资源的是FileWriter对象，缓冲区仅仅是一个提高效率的作用
//            //因此，此处的close()方法关闭的是被缓存的流对象
//            writeText.close();
//        } catch (FileNotFoundException e) {
//            System.out.println("没有找到指定文件");
//        } catch (IOException e) {
//            System.out.println("文件读写出错");
//        }
//    }
}