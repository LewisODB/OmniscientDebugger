package com.lambda.Debugger;

import static org.junit.Assert.assertNotNull;

import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.MemorySensitiveClassPathRepository;
import org.junit.Test;

public class CodePaneClassPathTest {
    @Test
    public void repeatedDependencyLookupKeepsRepositoryClassPathOpen() throws Exception {
        org.apache.bcel.util.Repository original = Repository.getRepository();
        ClassPath classPath = new ClassPath(System.getProperty("java.class.path"));
        Repository.setRepository(new MemorySensitiveClassPathRepository(classPath));
        try {
            assertNotNull(CodePane.lookupClassFile("org.apache.bcel.Repository"));
            assertNotNull(CodePane.lookupClassFile("org.apache.bcel.Repository"));
        } finally {
            Repository.setRepository(original);
            classPath.close();
        }
    }
}
