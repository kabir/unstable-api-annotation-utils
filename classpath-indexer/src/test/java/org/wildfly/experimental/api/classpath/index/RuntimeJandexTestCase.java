package org.wildfly.experimental.api.classpath.index;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.Experimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.NoUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.AnnotationAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.ClassAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.ConstructorAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.ConstructorParameterAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.FieldAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.InterfaceAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.MethodAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.MethodParameterAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.TypeConstructorBodyAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.TypeConstructorParameterAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.TypeFieldAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.TypeMethodBodyAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.TypeMethodParameterAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.TypeMethodReturnAnnotatedWithExperimental;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationOnUserClassUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationOnUserFieldUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationOnUserMethodUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassInfoScanner;
import org.wildfly.experimental.api.classpath.runtime.bytecode.JandexIndex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_FIELD;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_METHOD;

/**
 * Tests usage of annotations annotated with @Experimental.
 * For these we use Jandex for the lookup
 */
public class RuntimeJandexTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    ByteRuntimeIndex runtimeIndex;
    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                AnnotationWithExperimental.class);
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        runtimeIndex = ByteRuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        boolean ok = checkJandex(scanner, NoUsage.class);
        Assert.assertTrue(ok);
        Assert.assertEquals(0, scanner.getUsages().size());
    }

    @Test
    public void testClassAnnotationUsage() throws Exception {
        AnnotationOnUserClassUsage usage =
                checkJandexAndGetSingleAnnotationUsage(ClassAnnotatedWithExperimental.class, ANNOTATED_USER_CLASS)
                        .asAnnotationOnUserClassUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(ClassAnnotatedWithExperimental.class.getName(), usage.getClazz());
    }

    @Test
    public void testInterfaceAnnotationUsage() throws Exception {
        AnnotationOnUserClassUsage usage =
                checkJandexAndGetSingleAnnotationUsage(InterfaceAnnotatedWithExperimental.class, ANNOTATED_USER_CLASS)
                        .asAnnotationOnUserClassUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(InterfaceAnnotatedWithExperimental.class.getName(), usage.getClazz());
    }

    @Test
    public void testAnnotationAnnotationUsage() throws Exception {
        AnnotationOnUserClassUsage usage =
                checkJandexAndGetSingleAnnotationUsage(AnnotationAnnotatedWithExperimental.class, ANNOTATED_USER_CLASS)
                        .asAnnotationOnUserClassUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(AnnotationAnnotatedWithExperimental.class.getName(), usage.getClazz());
    }

    @Test
    public void testFieldAnnotationUsage() throws Exception {
        AnnotationOnUserFieldUsage usage =
                checkJandexAndGetSingleAnnotationUsage(FieldAnnotatedWithExperimental.class, ANNOTATED_USER_FIELD)
                        .asAnnotationOnUserFieldUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(FieldAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("field", usage.getFieldName());
    }

    @Test
    public void testMethodAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(MethodAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(MethodAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("method", usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
    }

    @Test
    public void testMethodParameterAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(MethodParameterAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(MethodParameterAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("method", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
    }

    @Test
    public void testConstructorAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(ConstructorAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(ConstructorAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
    }

    @Test
    public void testConstructorParameterAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(ConstructorParameterAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(ConstructorParameterAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
    }

    @Test
    public void testTypeFieldAnnotationUsage() throws Exception {
        AnnotationOnUserFieldUsage usage =
                checkJandexAndGetSingleAnnotationUsage(TypeFieldAnnotatedWithExperimental.class, ANNOTATED_USER_FIELD)
                        .asAnnotationOnUserFieldUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(TypeFieldAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("typeField", usage.getFieldName());
    }

    @Test
    public void testTypeMethodReturnAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(TypeMethodReturnAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();
        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(TypeMethodReturnAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("getList", usage.getMethodName());
        Assert.assertEquals("()Ljava/util/List;", usage.getDescriptor());
    }

    @Test
    public void testTypeMethodParameterAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(TypeMethodParameterAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();
        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(TypeMethodParameterAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("setList", usage.getMethodName());
        Assert.assertEquals("(Ljava/util/List;)V", usage.getDescriptor());
    }

    @Ignore("Jandex doesn't search method bodies")
    @Test
    public void testTypeMethodBodyAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(TypeMethodBodyAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();
        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(TypeMethodBodyAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals("createList", usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());

    }

    @Test
    public void testTypeConstructorParameterAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(TypeConstructorParameterAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();
        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(TypeConstructorParameterAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(Ljava/util/List;)V", usage.getDescriptor());

    }

    @Ignore("Jandex doesn't search method bodies")
    @Test
    public void testTypeConstructorBodyAnnotationUsage() throws Exception {
        AnnotationOnUserMethodUsage usage =
                checkJandexAndGetSingleAnnotationUsage(TypeConstructorBodyAnnotatedWithExperimental.class, ANNOTATED_USER_METHOD)
                        .asAnnotationOnUserMethodUsage();
        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(TypeConstructorBodyAnnotatedWithExperimental.class.getName(), usage.getClazz());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
    }


    String convertClassNameToVmFormat(Class<?> clazz) {
        return RuntimeIndex.convertClassNameToVmFormat(clazz.getName());
    }

    AnnotationUsage checkJandexAndGetSingleAnnotationUsage(
            Class<?> clazz, AnnotationUsageType type) throws IOException {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, clazz);
        checkJandex(scanner, clazz);
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

    private boolean checkJandex(ClassInfoScanner inspector, Class<?> clazz) throws IOException {
        Index index = Index.of(clazz);
        return inspector.checkAnnotationIndex(new JandexIndex() {
            @Override
            public Collection<AnnotationInstance> getAnnotations(String annotationName) {
                return index.getAnnotations(annotationName);
            }
        });
    }


}
