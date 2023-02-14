package com.example;

import ljystu.project.callgraph.util.JarReadUtil;
import org.junit.Test;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;


public class ReadClassFromJar {
    @Test
    public void readJarTest() throws IOException {
        String jarFilePath = "/Users/ljystu/Desktop/java-callgraph/target/javacg-0.1-SNAPSHOT-dycg-agent.jar";
        String tempDir = "src/main/resources/" + "javacg-0.1-SNAPSHOT-dycg-agent";

        try (ZipFile zipFile = new ZipFile(jarFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                File file = new File(tempDir, entry.getName());
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (InputStream input = zipFile.getInputStream(entry);
                     OutputStream output = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        Set<String> classes = JarReadUtil.getClasses(tempDir);
        System.out.println(classes.size());
        Set<String> importedPackages = new HashSet<>();
        try {
            importedPackages = ImportInfoReader.importInfo(jarFilePath, classes, stringBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String importedPackage : importedPackages) {
            System.out.println(importedPackage);
        }
        assertThat(importedPackages.size(), greaterThan(0));  // Fest assertion
    }
}
