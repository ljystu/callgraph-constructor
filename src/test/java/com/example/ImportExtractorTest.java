//package com.example;
//
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.Opcodes;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//public class ImportExtractorTest {
//
//    public static List<String> extractImports(InputStream classFile) throws IOException {
//        final List<String> imports = new ArrayList<>();
//        ClassReader classReader = new ClassReader(classFile);
//        classReader.accept(new ClassVisitor(Opcodes.ASM7) {
//            @Override
//            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//                super.visit(version, access, name, signature, superName, interfaces);
//            }
//
//            @Override
//            public void visitSource(String source, String debug) {
//                super.visitSource(source, debug);
//            }
//
//            @Override
//            public void visitOuterClass(String owner, String name, String desc) {
//                super.visitOuterClass(owner, name, desc);
//            }
//
//            @Override
//            public void visitInnerClass(String name, String outerName, String innerName, int access) {
//                super.visitInnerClass(name, outerName, innerName, access);
//            }
//
//            @Override
//            public void visitField(int access, String name, String desc, String signature, Object value) {
//                super.visitField(access, name, desc, signature, value);
//            }
//
//            @Override
//            public void visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//                super.visitMethod(access, name, desc, signature, exceptions);
//            }
//
//            @Override
//            public void visitImport(String desc, int access) {
//                imports.add(desc);
//                super.visitImport(desc, access);
//            }
//        }, 0);
//
//        return imports;
//    }
//}
