package org.wildfly.experimental.api.classpath.index;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.Experimental;
import org.wildfly.experimental.api.classpath.index.classes.usage.NoUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.annotation.AnnotatedClass;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotatedAnnotation;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.JandexIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATION_USAGE;

/**
 * Tests usage of annotations annotated with @Experimental.
 * For these we use Jandex for the lookup
 */
public class RuntimeJandexTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    RuntimeIndex runtimeIndex;
    @Before
    public void createRuntimeIndex() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                AnnotationWithExperimental.class);
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = checkJandex(inspector, NoUsage.class);
        Assert.assertTrue(ok);
        Assert.assertEquals(0, inspector.getUsages().size());
    }

    @Test
    public void testClassAnnotationUsage() throws Exception {
        AnnotatedAnnotation ann = checkJandexAndGetSingleAnnotationUsage(AnnotatedClass.class);
        Assert.assertEquals(1, ann.getAnnotations().size());
        Assert.assertTrue(ann.getAnnotations().contains(AnnotationWithExperimental.class.getName()));
    }

    String convertClassNameToVmFormat(Class<?> clazz) {
        return RuntimeIndex.convertClassNameToVmFormat(clazz.getName());
    }

    private AnnotatedAnnotation checkJandexAndGetSingleAnnotationUsage(
            Class<?> clazz) throws IOException {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = checkJandex(inspector, clazz);
        Assert.assertFalse(ok);
        Assert.assertEquals(1, inspector.getUsages().size());
        AnnotationUsage usage = inspector.getUsages().iterator().next();
        Assert.assertEquals(ANNOTATION_USAGE, usage.getType());
        return usage.asAnnotatedAnnotation();
    }


    private boolean checkJandex(ClassBytecodeInspector inspector, Class<?> clazz) throws IOException {
        Index index = Index.of(clazz);
        return inspector.checkAnnotationIndex(new JandexIndex() {
            @Override
            public Collection<AnnotationInstance> getAnnotations(String annotationName) {
                return index.getAnnotations(annotationName);
            }
        });
    }
}
