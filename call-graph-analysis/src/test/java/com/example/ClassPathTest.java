package com.example;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Iterator;

@Slf4j
public class ClassPathTest {
    @Test
    public void appendClassPathTest() {
        ClassPool pool = ClassPool.getDefault();
        ClassPath classPath = null;
        try {
            classPath = pool.appendClassPath("/Users/ljystu/Desktop/neo4j/junit4-main/lib/hamcrest-core-1.3.jar");
        } catch (NotFoundException e) {
            log.error("ClassPool append error:");

        }

        Iterator<String> importedPackages = pool.getImportedPackages();

        while (importedPackages.hasNext()) {
            String next = importedPackages.next();
            log.info(next);
        }
    }
}

