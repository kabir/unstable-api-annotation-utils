package org.wildfly.experimental.api.classpath.index;

import org.jboss.jandex.Index;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.index.java17.classes.Experimental;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.RecordNoUsage;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordConstructorCanonicalAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordConstructorCanonicalParameterAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordConstructorCompactAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordConstructorCompactParameterAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordConstructorParameterAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordGetterAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordMethodAnnotated;
import org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard.RecordMethodParameterAnnotated;
import org.wildfly.unstable.api.annotation.classpath.index.OverallIndex;
import org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotatedAnnotationUsage;
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

import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_ANNOTATION_USAGE;

public class RecordRuntimeJandexTestCase {

    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    RuntimeIndex runtimeIndex;
    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                AnnotationWithExperimental.class);
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());

        Path p = Paths.get("target/index/runtime-test.txt");
        overallIndex.save(p);

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        boolean ok = checkJandex(scanner, RecordNoUsage.class);
        Assert.assertTrue(ok);
        Assert.assertEquals(0, scanner.getUsages().size());
    }

    @Test
    public void testRecordAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordAnnotated.class.getName(), usage.getClazz());
    }

    @Test
    public void testRecordConstructorCanonicalAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordConstructorCanonicalAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordConstructorCanonicalAnnotated.class.getName(), usage.getClazz());
    }


    @Test
    public void testRecordConstructorCompactAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordConstructorCompactAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordConstructorCompactAnnotated.class.getName(), usage.getClazz());
    }

    @Test
    public void testRecordConstructorParameterAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordConstructorParameterAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordConstructorParameterAnnotated.class.getName(), usage.getClazz());
    }

    @Test
    public void testRecordConstructorCanonicalParameterAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordConstructorCanonicalParameterAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordConstructorCanonicalParameterAnnotated.class.getName(), usage.getClazz());
    }

    @Test
    public void testRecordConstructorCompactParameterAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordConstructorCompactParameterAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordConstructorCompactParameterAnnotated.class.getName(), usage.getClazz());
    }

    @Test
    public void testRecordGetterAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordGetterAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordGetterAnnotated.class.getName(), usage.getClazz());
    }

    @Test
    public void testRecordMethodAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordMethodAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordMethodAnnotated.class.getName(), usage.getClazz());
    }


    @Test
    public void testRecordMethodParameterAnnotationUsage() throws Exception {
        AnnotatedAnnotationUsage usage =
                checkJandexAndGetSingleAnnotationUsage(RecordMethodParameterAnnotated.class, ANNOTATED_ANNOTATION_USAGE)
                        .asAnnotatedAnnotationUsage();

        Assert.assertEquals(1, usage.getAnnotations().size());
        Assert.assertTrue(usage.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
        Assert.assertEquals(RecordMethodParameterAnnotated.class.getName(), usage.getClazz());
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
        URL url = RecordRuntimeJandexTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            scanner.scanClass(in);
        }
    }

    private boolean checkJandex(ClassInfoScanner inspector, Class<?> clazz) throws IOException {
        Index index = Index.of(clazz);
        return inspector.checkAnnotationIndex(annotationName -> index.getAnnotations(annotationName));
    }


}
