package org.wildfly.unstable.api.annotation.classpath.index;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.unstable.api.annotation.classpath.index.classes.Experimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.InstanceMethodReferenceUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.MethodsClass;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.StaticMethodReferenceUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.usage.NoUsage;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotatedMethodReference;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsage;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.ClassInfoScanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

/**
 * This is to
 */
public class MethodReferencesTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    RuntimeIndex runtimeIndex;

    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                InstanceMethodReferenceUsage.class,
                MethodsClass.class,
                StaticMethodReferenceUsage.class);
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION);

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, NoUsage.class);
        Assert.assertEquals(0, scanner.getUsages().size());
    }

    @Test
    public void testInstanceMethodReferenceUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(InstanceMethodReferenceUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(InstanceMethodReferenceUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodsClass.class.getName(), usage.getMethodClass());
        Assert.assertEquals("instanceConcatWithExperimental", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }



    AnnotationUsage scanAndGetSingleAnnotationUsage(
            Class<?> clazz,
            AnnotationUsageType type) throws IOException {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, clazz);

        Assert.assertEquals(1, scanner.getUsages().size());
        AnnotationUsage usage = scanner.getUsages().iterator().next();
        Assert.assertEquals(type, usage.getType());
        return usage;
    }

    private void scanClass(ClassInfoScanner scanner, Class<?> clazz) throws IOException {
        String classLocation = clazz.getName().replaceAll("\\.", "/") + ".class";
        URL url = ClassInfoScannerTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            scanner.scanClass(in);
        }
    }

}
