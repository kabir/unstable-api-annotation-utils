package org.wildfly.experimental.api.classpath.index;

import org.jboss.jandex.Indexer;
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
import org.wildfly.experimental.api.classpath.index.classes.usage.ConstructorReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.FieldReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotatedFieldReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotatedMethodReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;
import org.wildfly.experimental.api.classpath.runtime.bytecode.JandexCollector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

public class JandexCollectorTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    ByteRuntimeIndex runtimeIndex;

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

        runtimeIndex = ByteRuntimeIndex.load(p);
    }


    @Test
    public void testConstructorReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ConstructorReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(ConstructorReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalConstructors.class.getName(), usage.getMethodClass());
        Assert.assertEquals(ClassBytecodeInspector.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testFieldReference() throws Exception {
        AnnotatedFieldReference usage =
                scanAndGetSingleAnnotationUsage(FieldReference.class, FIELD_REFERENCE)
                        .asAnnotatedFieldReference();

        Assert.assertEquals(FieldReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalFields.class.getName(), usage.getFieldClass());
        Assert.assertEquals("fieldA", usage.getFieldName());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    AnnotationUsage scanAndGetSingleAnnotationUsage(
            Class<?> clazz,
            AnnotationUsageType type) throws IOException {
        JandexCollector jandexCollector = new JandexCollector(runtimeIndex);
        Indexer indexer = new Indexer(jandexCollector);
        scanClass(indexer, clazz);

        Assert.assertEquals(1, jandexCollector.getUsages().size());
        AnnotationUsage usage = jandexCollector.getUsages().iterator().next();
        Assert.assertEquals(type, usage.getType());
        return usage;
    }

    private void scanClass(Indexer indexer, Class<?> clazz) throws IOException {
        String classLocation = clazz.getName().replaceAll("\\.", "/") + ".class";
        URL url = RuntimeReferenceTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            indexer.index(in);
        }
    }

}
