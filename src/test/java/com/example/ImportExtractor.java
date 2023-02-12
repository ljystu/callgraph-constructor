//package com.example;
//
//import org.objectweb.asm.ClassReader;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class ImportExtractor {
//    public static void main(String[] args) throws IOException {
//
//        InputStream inputStream = ImportExtractor.class.getResourceAsStream("/Users/ljystu/Desktop/neo4j/target/classes/ljystu/project/callgraph/RunMavenInvoker.class");
//        ClassReader classReader = new ClassReader(inputStream);
//        classReader.accept(new ImportVisitor(), ClassReader.EXPAND_FRAMES);
//
//    }
//}
