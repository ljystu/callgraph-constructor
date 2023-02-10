package com.example;

import java.io.IOException;

import com.ibm.wala.shrike.shrikeBT.shrikeCT.tools.ClassPrinter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.reflect.Method;


public class ASMTest extends ClassVisitor {
    public ASMTest() {
        super(Opcodes.ASM9);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println("Method name: " + name + ", Descriptor: " + descriptor);
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    public static void main(String[] args) throws IOException {
        ASMTest classPrinter = new ASMTest();
        ClassReader reader = new ClassReader("java.lang.String");
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        System.out.println("Class name: " + classNode.name);

        for (MethodNode methodNode : classNode.methods) {
            System.out.println(methodNode.desc);
//            System.out.println("Method name: " + methodNode.name);
//            System.out.println("Return type: " + methodNode.desc);
//            methodNode.attrs.forEach(System.out::println);
            System.out.println(methodNode);
//                    .substring(methodNode.desc.lastIndexOf(")") + 1));
//            System.out.println("Parameter types: " + methodNode.desc.substring(1, methodNode.desc.indexOf(")")));
        }
    }

}
