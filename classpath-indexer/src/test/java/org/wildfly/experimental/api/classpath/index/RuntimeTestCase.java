package org.wildfly.experimental.api.classpath.index;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalConstructors;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalFields;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.Experimental;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassExtendsUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassImplementsUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.ConstructorReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.FieldReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.MethodReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.NoUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.StaticFieldReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.StaticMethodReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotatedFieldReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotatedMethodReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.ExtendsAnnotatedClass;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.ImplementsAnnotatedInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.EXTENDS_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.IMPLEMENTS_INTERFACE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.METHOD_REFERENCE;

public class RuntimeTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    RuntimeIndex runtimeIndex;
    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                AnnotationWithExperimental.class,
                ClassWithExperimental.class,
                InterfaceWithExperimental.class,
                ClassWithExperimentalMethods.class,
                InterfaceWithExperimentalMethods.class,
                AnnotationWithExperimentalMethods.class,
                ClassWithExperimentalConstructors.class,
                ClassWithExperimentalFields.class);
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = scanClass(inspector, NoUsage.class);
        Assert.assertTrue(ok);
        Assert.assertEquals(0, inspector.getUsages().size());
    }

    @Test
    public void testClassExtendsUsage() throws Exception {
        ExtendsAnnotatedClass usage =
                scanAndGetSingleAnnotationUsage(ClassExtendsUsage.class, EXTENDS_CLASS, ExtendsAnnotatedClass.class);

        Assert.assertEquals(convertClassNameToVmFormat(ClassExtendsUsage.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimental.class), usage.getSuperClass());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testClassImplementsUsage() throws Exception {
        ImplementsAnnotatedInterface usage =
                scanAndGetSingleAnnotationUsage(ClassImplementsUsage.class, IMPLEMENTS_INTERFACE, ImplementsAnnotatedInterface.class);

        Assert.assertEquals(convertClassNameToVmFormat(ClassImplementsUsage.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(InterfaceWithExperimental.class), usage.getInterface());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testConstructorReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ConstructorReference.class, METHOD_REFERENCE, AnnotatedMethodReference.class);

        Assert.assertEquals(convertClassNameToVmFormat(ConstructorReference.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimentalConstructors.class), usage.getMethodClass());
        Assert.assertEquals(ClassBytecodeInspector.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testFieldReference() throws Exception {
        AnnotatedFieldReference usage =
                scanAndGetSingleAnnotationUsage(FieldReference.class, FIELD_REFERENCE, AnnotatedFieldReference.class);

        Assert.assertEquals(convertClassNameToVmFormat(FieldReference.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimentalFields.class), usage.getFieldClass());
        Assert.assertEquals("fieldA", usage.getFieldName());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testStaticFieldReference() throws Exception {
        AnnotatedFieldReference usage =
                scanAndGetSingleAnnotationUsage(StaticFieldReference.class, FIELD_REFERENCE, AnnotatedFieldReference.class);

        Assert.assertEquals(convertClassNameToVmFormat(StaticFieldReference.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimentalFields.class), usage.getFieldClass());
        Assert.assertEquals("fieldB", usage.getFieldName());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testMethodReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(MethodReference.class, METHOD_REFERENCE, AnnotatedMethodReference.class);

        Assert.assertEquals(convertClassNameToVmFormat(MethodReference.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimentalMethods.class), usage.getMethodClass());
        Assert.assertEquals("test", usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testStaticMethodReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(StaticMethodReference.class, METHOD_REFERENCE, AnnotatedMethodReference.class);

        Assert.assertEquals(convertClassNameToVmFormat(StaticMethodReference.class), usage.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimentalMethods.class), usage.getMethodClass());
        Assert.assertEquals("test", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }



    <T extends AnnotationUsage> T scanAndGetSingleAnnotationUsage(
            Class<?> clazz,
            ClassBytecodeInspector.AnnotationUsageType type,
            Class<T> usageClass) throws IOException {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = scanClass(inspector, clazz);
        Assert.assertFalse(ok);
        Assert.assertEquals(1, inspector.getUsages().size());
        AnnotationUsage usage = inspector.getUsages().iterator().next();
        Assert.assertEquals(type, usage.getType());
        return usageClass.cast(usage);
    }

    String convertClassNameToVmFormat(Class<?> clazz) {
        return RuntimeIndex.convertClassNameToVmFormat(clazz.getName());
    }

    private boolean scanClass(ClassBytecodeInspector inspector, Class<?> clazz) throws IOException {
        String classLocation = clazz.getName().replaceAll("\\.", "/") + ".class";
        URL url = RuntimeTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            return inspector.scanClassFile(clazz.getName(), in);
        }
    }
}
