package org.wildfly.experimental.api.classpath.index;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.index.java17.classes.Experimental;
import org.wildfly.experimental.api.classpath.index.java17.classes.InterfaceWithExperimental;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordImplementsAnnotatedInterface;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedConstructor;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedConstructorParameter;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedGetter;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedGetterFromConstructorParameter;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedMethod;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedMethodParameter;
import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedStaticField;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.ConstructorReference;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.ConstructorWithParameterUsage;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.GetterWithAnnotationFromConstructorUsage;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.GetterWithAnnotationUsage;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.MethodReference;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.MethodWithParameterReference;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.RecordNoUsage;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.StaticFieldUsage;
import org.wildfly.unstable.api.annotation.classpath.index.OverallIndex;
import org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotatedFieldReference;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotatedMethodReference;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsage;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.ClassInfoScanner;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.ImplementsAnnotatedInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.IMPLEMENTS_INTERFACE;
import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

public class RecordScannerTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    RuntimeIndex runtimeIndex;

    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                AnnotationWithExperimental.class,
                ConstructorReference.class,
                ConstructorWithParameterUsage.class,
                GetterWithAnnotationUsage.class,
                GetterWithAnnotationFromConstructorUsage.class,
                MethodReference.class,
                MethodWithParameterReference.class,
                StaticFieldUsage.class,
                InterfaceWithExperimental.class,
                RecordNoUsage.class,
                RecordImplementsAnnotatedInterface.class,
                RecordWithAnnotatedConstructor.class,
                RecordWithAnnotatedConstructorParameter.class,
                RecordWithAnnotatedGetter.class,
                RecordWithAnnotatedGetterFromConstructorParameter.class,
                RecordWithAnnotatedMethod.class,
                RecordWithAnnotatedMethodParameter.class,
                RecordWithAnnotatedStaticField.class);
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION);

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, RecordNoUsage.class);
        Assert.assertEquals(0, scanner.getUsages().size());
    }


    @Test
    public void testClassImplementsUsage() throws Exception {
        ImplementsAnnotatedInterface usage =
                scanAndGetSingleAnnotationUsage(RecordImplementsAnnotatedInterface.class, IMPLEMENTS_INTERFACE)
                        .asImplementsAnnotatedInterface();

        Assert.assertEquals(RecordImplementsAnnotatedInterface.class.getName(), usage.getSourceClass());
        Assert.assertEquals(InterfaceWithExperimental.class.getName(), usage.getInterface());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }


    @Test
    public void testConstructorReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ConstructorReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(ConstructorReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedConstructor.class.getName(), usage.getMethodClass());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(I)V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testConstructorWithAnnotatedConstructorParameter() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ConstructorWithParameterUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(ConstructorWithParameterUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedConstructorParameter.class.getName(), usage.getMethodClass());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(I)V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testAnnotatedField() throws Exception {
        // Instance fields are not allowed in records, so we only have a static field
        AnnotatedFieldReference usage =
                scanAndGetSingleAnnotationUsage(StaticFieldUsage.class, FIELD_REFERENCE)
                        .asAnnotatedFieldReference();

        Assert.assertEquals(StaticFieldUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedStaticField.class.getName(), usage.getFieldClass());
        Assert.assertEquals("staticField", usage.getFieldName());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }
    @Test
    public void testAnnotatedGetter() throws Exception {
        // Instance fields are not allowed in records, so we only have a static field
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(GetterWithAnnotationUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(GetterWithAnnotationUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedGetter.class.getName(), usage.getMethodClass());
        Assert.assertEquals("i", usage.getMethodName());
        Assert.assertEquals("()I", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testAnnotatedGetterFromConstructorParameter() throws Exception {
        // Instance fields are not allowed in records, so we only have a static field
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(GetterWithAnnotationFromConstructorUsage.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(GetterWithAnnotationFromConstructorUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedGetterFromConstructorParameter.class.getName(), usage.getMethodClass());
        Assert.assertEquals("i", usage.getMethodName());
        Assert.assertEquals("()I", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testAnnotatedMethod() throws Exception {
        // Instance fields are not allowed in records, so we only have a static field
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(MethodReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(MethodReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedMethod.class.getName(), usage.getMethodClass());
        Assert.assertEquals("method", usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testAnnotatedMethodParameter() throws Exception {
        // Instance fields are not allowed in records, so we only have a static field
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(MethodWithParameterReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(MethodWithParameterReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(RecordWithAnnotatedMethodParameter.class.getName(), usage.getMethodClass());
        Assert.assertEquals("method", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
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
        URL url = RecordScannerTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            scanner.scanClass(in);
        }
    }
}
