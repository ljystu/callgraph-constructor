/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ljystu.javacg.dyn;

import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Instrumenter implements ClassFileTransformer {


    static List<Pattern> pkgIncl = new ArrayList<>();
    static List<Pattern> pkgExcl = new ArrayList<>();
    static String redisKey;
    static String packagePrefix;

    static String info = "";

    public static void premain(String argument, Instrumentation instrumentation) {

        // incl=com.foo.*,gr.bar.foo;excl=com.bar.foo.*

        String[] split = argument.split("!");
        redisKey = split[1];
        packagePrefix = split[0];
        String[] prefixes = packagePrefix.split(",");
        for (String prefix : prefixes) {
            prefix += ".*";
            pkgIncl.add(Pattern.compile(prefix));
        }

//        if (argument == null) {
//            err("Missing configuration argument");
//            return;
//        }
//
//        err("Argument is: " + argument);
//
//        String[] tokens = argument.split(";");
//
//        if (tokens.length < 1) {
//            err("Missing delimiter ;");
//            return;
//        }
//
//        for (String token : tokens) {
//            String[] args = token.split("=");
//            if (args.length < 2) {
//                err("Missing argument delimiter =:" + token);
//                return;
//            }
//
//            String argtype = args[0];
//            if (!argtype.equals("incl") && !argtype.equals("excl") && !argtype.equals("info")) {
//                err("Wrong argument: " + argtype);
//                return;
//            }
//
//            if (argtype.equals("info")) {
//                info = args[1];
//
//                break;
//            }
//            String[] patterns = args[1].split(",");
//
//            for (String pattern : patterns) {
//                Pattern p = null;
//                err("Compiling " + argtype + " pattern:" + pattern + "$");
//                try {
//                    p = Pattern.compile(pattern + "$");
//                } catch (PatternSyntaxException pse) {
//                    err("pattern: " + pattern + " not valid, ignoring");
//                }
//                if (argtype.equals("incl"))
//                    pkgIncl.add(p);
//                else
//                    pkgExcl.add(p);
//            }


//        }

        instrumentation.addTransformer(new Instrumenter());
        err("transformer added");
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> clazz,
                            java.security.ProtectionDomain domain, byte[] bytes) {
        boolean enhanceClass = true;
        String name = className.replace("/", ".");

//        System.err.println(className +" checked");
//        for (Pattern p : pkgIncl) {
//            Matcher m = p.matcher(name);
//            if (m.matches()) {
//                enhanceClass = true;
//                break;
//            }
//        }
//
//        for (Pattern p : pkgExcl) {
//            Matcher m = p.matcher(name);
//            if (m.matches()) {
//                err("Skipping class: " + name);
//                enhanceClass = false;
//                break;
//            }
//        }
//        try {
//            File file = new File("/Users/ljystu/Desktop/jedis-2.9.1.jar");
//            ClassPool cp = ClassPool.getDefault();
//            cp.insertClassPath(file.getAbsolutePath());
//        } catch (NotFoundException e) {
//            throw new RuntimeException(e);
//        }

        if (enhanceClass) {
            return enhanceClass(className, bytes);
        } else {
            return bytes;
        }
    }

    private byte[] enhanceClass(String name, byte[] b) {
        CtClass clazz = null;
        try {
            ClassPool pool = ClassPool.getDefault();

            clazz = pool.makeClass(new ByteArrayInputStream(b));
            if (!clazz.isInterface()) {
                err("Enhancing class: " + name);
                CtBehavior[] methods = clazz.getDeclaredBehaviors();
                for (int i = 0; i < methods.length; i++) {
                    if (!methods[i].isEmpty()) {
                        enhanceMethod(methods[i], clazz.getName());
                    }
                }
                b = clazz.toBytecode();
            }
        } catch (CannotCompileException e) {
            e.printStackTrace();
            err("Cannot compile: " + e.getMessage());
        } catch (NotFoundException e) {
            e.printStackTrace();
            err("Cannot find: " + e.getMessage());
        } catch (IOException e) {
            err("Error writing: " + e.getMessage());
        } finally {
            if (clazz != null) {
                clazz.detach();
            }
        }
        return b;
    }

    private void enhanceMethod(CtBehavior method, String clazzName)
            throws NotFoundException, CannotCompileException {
        String name = clazzName.substring(clazzName.lastIndexOf('.') + 1);
//        System.out.println(clazzName);
        String methodName = method.getName();

        String params = getMethodArgNames(method);
        int lastDot = clazzName.lastIndexOf('.');
        String packageName = clazzName.substring(0, lastDot);
        String className = clazzName.substring(lastDot + 1).replace('.', '/');
//        className = className.;
//        clazzName = packageName + className;
        if (method.getName().equals(name))
            methodName = "<init>";
        try {
            method.insertBefore("dyn.ljystu.javacg.MethodStack.push(\"" + packageName + ":" + className
                    + ":" + methodName + ":" + params + "\");");

            method.insertAfter("dyn.ljystu.javacg.MethodStack.pop(\"" + info + "\");");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getMethodArgNames(CtBehavior method) {
        try {

            CtMethod cm = (CtMethod) method;

            // 使用javaassist的反射方法获取方法的参数名
            MethodInfo methodInfo = cm.getMethodInfo();
            MethodInfo methodInfo1 = cm.getMethodInfo();
            CtClass[] parameterTypes = cm.getParameterTypes();
            CtClass returnType = cm.getReturnType();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            if (attr == null) {
                throw new RuntimeException("LocalVariableAttribute of method is null! Class " + ",method name is ");
            }
            String[] paramNames = new String[cm.getParameterTypes().length];
            int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < paramNames.length; i++) {
                str.append(parameterTypes[i].getName());
                if (i != paramNames.length - 1) {
                    str.append(",");
                }
            }
//                        attr.variableName(i + pos) + ",";
//            str.append(")");
            str.append("&").append(returnType.getName());

            return str.toString();
        } catch (NotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void err(String msg) {
//        System.err.println("[JAVACG-DYN] " + msg);
    }
}