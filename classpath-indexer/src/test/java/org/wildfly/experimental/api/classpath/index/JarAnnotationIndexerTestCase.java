package org.wildfly.experimental.api.classpath.index;

import org.junit.Assert;
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

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class JarAnnotationIndexerTestCase {

    // These feel a bit like corner cases, especially for our first iteration
    // TODO Method parameters
    // TODO Method/Constructor parameters

    // TODO These are more of a runtime check thing I think? The classes will have been annotated
    // implemented interface
    // Super class


    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();

    @Test
    public void testScanClassLevelAnnotations() throws Exception {
        File file = TestUtils.createJar(AnnotationWithExperimental.class, ClassWithExperimental.class, InterfaceWithExperimental.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        checkSet(result.getAnnotatedAnnotations(), AnnotationWithExperimental.class.getName());
        checkSet(result.getAnnotatedClasses(), ClassWithExperimental.class.getName());
        checkSet(result.getAnnotatedInterfaces(), InterfaceWithExperimental.class.getName());
    }

    @Test
    public void testScanMethodAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalMethods.class, InterfaceWithExperimentalMethods.class, AnnotationWithExperimentalMethods.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedMethod> set = result.getAnnotatedMethods();
        Assert.assertEquals(5, set.size());

        Assert.assertTrue(set.contains(new AnnotatedMethod(AnnotationWithExperimentalMethods.class.getName(), "value", "()Ljava/lang/String;")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), "test", "()V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), "test", "()V")));
    }
    @Test
    public void testScanConstructorAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalConstructors.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedConstructor> set = result.getAnnotatedConstructors();
        Assert.assertEquals(2, set.size());

        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "()V")));
    }

    @Test
    public void testScanFieldAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalFields.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
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
