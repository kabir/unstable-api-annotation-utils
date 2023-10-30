package org.wildfly.experimental.api.classpath.indexer;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.experimental.api.classpath.indexer.JarAnnotationIndexerResult.AnnotatedField;
import org.wildfly.experimental.api.classpath.indexer.JarAnnotationIndexerResult.AnnotatedMethod;
import org.wildfly.experimental.api.classpath.indexer.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.indexer.classes.AnnotationWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.indexer.classes.ClassWithExperimental;
import org.wildfly.experimental.api.classpath.indexer.classes.ClassWithExperimentalFields;
import org.wildfly.experimental.api.classpath.indexer.classes.ClassWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.indexer.classes.Experimental;
import org.wildfly.experimental.api.classpath.indexer.classes.InterfaceWithExperimental;
import org.wildfly.experimental.api.classpath.indexer.classes.InterfaceWithExperimentalMethods;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.indexer.JarAnnotationIndexerResult.ClassType.ANNOTATION;
import static org.wildfly.experimental.api.classpath.indexer.JarAnnotationIndexerResult.ClassType.CLASS;
import static org.wildfly.experimental.api.classpath.indexer.JarAnnotationIndexerResult.ClassType.INTERFACE;

public class JarAnnotationIndexerTestCase {

    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    @Test
    public void testScanClassLevelAnnotations() throws Exception {
        File file = TestUtils.createJar(AnnotationWithExperimental.class, ClassWithExperimental.class, InterfaceWithExperimental.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndexerResult result = indexer.scanForAnnotation();
        checkSet(result.getAnnotatedAnnotations(), AnnotationWithExperimental.class.getName());
        checkSet(result.getAnnotatedClasses(), ClassWithExperimental.class.getName());
        checkSet(result.getAnnotatedInterfaces(), InterfaceWithExperimental.class.getName());
    }

    @Test
    public void testScanMethodAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalMethods.class, InterfaceWithExperimentalMethods.class, AnnotationWithExperimentalMethods.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndexerResult result = indexer.scanForAnnotation();
        Set<AnnotatedMethod> set = result.getAnnotatedMethods();
        Assert.assertEquals(5, set.size());

        Assert.assertTrue(set.contains(new AnnotatedMethod(AnnotationWithExperimentalMethods.class.getName(), ANNOTATION, "value", "()Ljava/lang/String;")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), CLASS, "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), CLASS, "test", "()V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), INTERFACE, "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), INTERFACE, "test", "()V")));


        // TODO Constructor

        // TODO Method parameters
        // TODO Method/Constructor parameters

        // TODO These are more of a runtime check thing I think? The classes will have been annotated
        // implemented interface
        // Super class

    }

    @Test
    public void testScanFieldAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalFields.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndexerResult result = indexer.scanForAnnotation();
        Set<AnnotatedField> set = result.getAnnotatedFields();
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains(new AnnotatedField(ClassWithExperimentalFields.class.getName(), "fieldA")));
        Assert.assertTrue(set.contains(new AnnotatedField(ClassWithExperimentalFields.class.getName(), "fieldB")));
    }


    private void checkSet(Set<String> set, String... expected) {
        Assert.assertEquals(expected.length, set.size());
        for (String s : expected) {
            Assert.assertTrue(set.contains(s));
        }
    }
}
