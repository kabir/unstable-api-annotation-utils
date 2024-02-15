package org.wildfly.unstable.api.annotation.classpath.index;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.unstable.api.annotation.classpath.index.classes.Experimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.ClassInstanceMethodHandleUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.ConstructorMethodHandleUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.InterfaceDefaultMethodHandleUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.InterfaceInstanceMethodHandleUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.InterfaceStaticMethodHandleUsage;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.MethodHandlesClass;
import org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences.ClassStaticMethodHandleUsage;
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
 * <p>This is to test the CONSTANT_MethodHandle_info references.</p>
 *
 * <p>It is a bit involved so it is separate from ClassInfoScannerTestCase. There is no special handling
 * of these in the bytecode scanner. Essentially a CONSTANT_MethodHandle_info entry will have a reference
 * to a CONSTANT_FieldRef_Info, CONSTANT_MethodRed_Info or a CONSTANT_InterfaceMethodRef_Info.</p>
 *
 * <p>Since all those appear as separate entries in the constant pool they will get picked up by the scanner
 * automatically. Since we test that thoroughly elsewhere, there is no separate test for annotations which
 * have {@code ElementType.TYPE_USE}.</p>
 *
 * <p>I could not find a way to test static and instance field writes and reads using MethodHandles, but again
 * this would result in a referenced CONSTANT_FieldRef_Info also in the constant pool. Although it would be nice
 * to test this here, it is tested well elsewhere.</p>
 */
public class MethodHandlesTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    RuntimeIndex runtimeIndex;

    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                ClassInstanceMethodHandleUsage.class,
                MethodHandlesClass.class,
                ClassStaticMethodHandleUsage.class);
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
    public void testClassInstanceMethodHandleUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ClassInstanceMethodHandleUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(ClassInstanceMethodHandleUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodHandlesClass.class.getName(), usage.getMethodClass());
        Assert.assertEquals("instanceConcatWithExperimental", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testClassStaticMethodHandleUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(InterfaceStaticMethodHandleUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(InterfaceStaticMethodHandleUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodHandlesClass.Concat.class.getName(), usage.getMethodClass());
        Assert.assertEquals("staticConcatWithExperimental", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testInterfaceInstanceMethodHandleUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(InterfaceInstanceMethodHandleUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(InterfaceInstanceMethodHandleUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodHandlesClass.Concat.class.getName(), usage.getMethodClass());
        Assert.assertEquals("instanceConcatWithExperimental", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testInterfaceStaticMethodHandleUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(InterfaceStaticMethodHandleUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(InterfaceStaticMethodHandleUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodHandlesClass.Concat.class.getName(), usage.getMethodClass());
        Assert.assertEquals("staticConcatWithExperimental", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testInterfaceDefaultMethodHandleUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(InterfaceDefaultMethodHandleUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(InterfaceDefaultMethodHandleUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodHandlesClass.Concat.class.getName(), usage.getMethodClass());
        Assert.assertEquals("defaultConcatWithExperimental", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testConstructorMethodHandleUsage() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ConstructorMethodHandleUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(ConstructorMethodHandleUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(MethodHandlesClass.ClassWithExperimentalConstructor.class.getName(), usage.getMethodClass());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
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
