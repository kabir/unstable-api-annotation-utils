package org.wildfly.experimental.api.classpath.indexer;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.indexer.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.indexer.classes.ClassWithExperimental;
import org.wildfly.experimental.api.classpath.indexer.classes.Experimental;
import org.wildfly.experimental.api.classpath.indexer.classes.InterfaceWithExperimental;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class JarAnnotationIndexerTestCase {

    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    @Test
    public void testScanAnnotations() throws Exception {
        File file = TestUtils.createJar(AnnotationWithExperimental.class, ClassWithExperimental.class, InterfaceWithExperimental.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndexerResult result = indexer.scanForAnnotation();
        checkSet(result.getAnnotatedAnnotations(), AnnotationWithExperimental.class.getName());
        checkSet(result.getAnnotatedClasses(), ClassWithExperimental.class.getName());
        checkSet(result.getAnnotatedInterfaces(), InterfaceWithExperimental.class.getName());
    }

    private void checkSet(Set<String> set, String... expected) {
        Assert.assertEquals(expected.length, set.size());
        for (String s : expected) {
            Assert.assertTrue(set.contains(s));
        }
    }
}
