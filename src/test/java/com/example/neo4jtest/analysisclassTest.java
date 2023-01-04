package com.example.neo4jtest;

import com.example.neo4jtest.util.ExampleUtil;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import org.junit.Test;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

//类分析
public class analysisclassTest {

    private static final String FILEPATH =
//            "/Users/ljystu/Desktop/wakatest/src/main/resources/zookeeper-3.6.0.jar";
            "/Users/ljystu/Desktop/wakatest/src/main/resources/wakatest.jar";
//            "/Users/ljystu/Downloads/jar_files/mysql-connector-java-8.0.11.jar";

    public static void classanalysis() throws IOException, WalaException, CancelException {
        long start = System.currentTimeMillis();
        AnalysisScope scope = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(FILEPATH,
                (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

//        callGraphTest(scope);
//        PDFtest(FILEPATH, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

        long end = System.currentTimeMillis();
        System.out.println("took " + (end - start) + "ms");

    }

    private static void callGraphTest(AnalysisScope scope) throws IOException, CallGraphBuilderCancelException, ClassHierarchyException {
        ExampleUtil.addDefaultExclusions(scope);
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);

//        System.out.println(cha.getNumberOfClasses() + " classes");
//        System.out.println(Warnings.asString());

//        Warnings.clear();
        AnalysisOptions options = new AnalysisOptions();
        String entryClass = null;
        String mainClass = "LMain";
//        Iterable<Entrypoint> entrypoints = entryClass != null ? makePublicEntrypoints(cha, entryClass) : Util.makeMainEntrypoints(cha, mainClass);
        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha);
//        Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
        options.setEntrypoints(entrypoints);

        // you can dial down reflection handling if you like
//    options.setReflectionOptions(ReflectionOptions.NONE);
        AnalysisCache cache = new AnalysisCacheImpl();
        // other builders can be constructed with different Util methods
        CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha);
//    CallGraphBuilder builder = Util.makeNCFABuilder(2, options, cache, cha, scope);
//    CallGraphBuilder builder = Util.makeVanillaNCFABuilder(2, options, cache, cha, scope);


        System.out.println("building call graph...");
        CallGraph cg = builder.makeCallGraph(options, null);

        CGNode node = cg.getNode(10);
//        System.out.println(cg.);
        System.out.println(node.getMethod());
        ;
        System.out.println(cg.getNumberOfNodes());
//        System.out.println(cg.getClassHierarchy());

        System.out.println("done");

        System.out.println(CallGraphStats.getStats(cg));

    }

    private static void PDFtest(String FILEPATH, File exclusions) throws WalaException, CancelException, IOException {
        Process run = PDFCallGraph.run(FILEPATH, CallGraphTestUtil.REGRESSION_EXCLUSIONS);

    }

    private static Iterable<Entrypoint> makePublicEntrypoints(IClassHierarchy cha, String entryClass) {
        Collection<Entrypoint> result = new ArrayList<>();
        IClass klass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application,
                StringStuff.deployment2CanonicalTypeString(entryClass)));
        for (IMethod m : klass.getDeclaredMethods()) {
            if (m.isPublic()) {
                result.add(new DefaultEntrypoint(m, cha));
            }
        }
        return result;
    }

    @Test
    public void myTest() throws IOException, WalaException, CancelException {
        classanalysis();
    }
}