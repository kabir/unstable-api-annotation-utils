package org.wildfly.unstable.api.annotation.classpath.index;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.unstable.api.annotation.classpath.index.classes.AnnotationWithExperimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.AnnotationWithExperimentalMethods;
import org.wildfly.unstable.api.annotation.classpath.index.classes.AnnotationWithExperimentalMethodsTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.AnnotationWithExperimentalTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalConstructors;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalConstructorsTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalFields;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalFieldsTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalMethods;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalMethodsTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.Experimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ExperimentalTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.InterfaceWithExperimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.InterfaceWithExperimentalMethods;
import org.wildfly.unstable.api.annotation.classpath.index.classes.InterfaceWithExperimentalMethodsTypeUse;
import org.wildfly.unstable.api.annotation.classpath.index.classes.InterfaceWithExperimentalTypeUse;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class JarAnnotationIndexerTestCase {

    // These feel a bit like corner cases, especially for our first iteration
    // TODO Method parameters
    // TODO Method/Constructor parameters


    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    private static final String EXPERIMENTAL_ANNOTATION_WITH_TYPE_USE = ExperimentalTypeUse.class.getName();

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
    public void testScanClassLevelAnnotationsWithTypeUse() throws Exception {
        // The Jandex lookup works differently when TYPE_USE is one of the targets
        File file = TestUtils.createJar(AnnotationWithExperimentalTypeUse.class, ClassWithExperimentalTypeUse.class, InterfaceWithExperimentalTypeUse.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION_WITH_TYPE_USE, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        checkSet(result.getAnnotatedAnnotations(), AnnotationWithExperimentalTypeUse.class.getName());
        checkSet(result.getAnnotatedClasses(), ClassWithExperimentalTypeUse.class.getName());
        checkSet(result.getAnnotatedInterfaces(), InterfaceWithExperimentalTypeUse.class.getName());
    }

    @Test
    public void testScanMethodAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalMethods.class, InterfaceWithExperimentalMethods.class, AnnotationWithExperimentalMethods.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedMethod> set = result.getAnnotatedMethods();
        Assert.assertEquals(7, set.size());

        Assert.assertTrue(set.contains(new AnnotatedMethod(AnnotationWithExperimentalMethods.class.getName(), "value", "()Ljava/lang/String;")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), "test", "()V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), "methodWithExperimentalParameter", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), "test", "()V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), "methodWithExperimentalParameter", "(Ljava/lang/String;)V")));
    }

    @Test
    public void testScanMethodAnnotationsWithTypeUse() throws Exception {
        // The Jandex lookup works differently when TYPE_USE is one of the targets
        File file = TestUtils.createJar(ClassWithExperimentalMethodsTypeUse.class, InterfaceWithExperimentalMethodsTypeUse.class, AnnotationWithExperimentalMethodsTypeUse.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION_WITH_TYPE_USE, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedMethod> set = result.getAnnotatedMethods();
        Assert.assertEquals(11, set.size());

        Assert.assertTrue(set.contains(new AnnotatedMethod(AnnotationWithExperimentalMethodsTypeUse.class.getName(), "value", "()Ljava/lang/String;")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethodsTypeUse.class.getName(), "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethodsTypeUse.class.getName(), "test", "()V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethodsTypeUse.class.getName(), "methodWithExperimentalParameter", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethodsTypeUse.class.getName(), "methodWithExperimentalTypeParameter", "(Ljava/util/List;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(ClassWithExperimentalMethodsTypeUse.class.getName(), "methodWithExperimentalTypeReturn", "()Ljava/util/List;")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethodsTypeUse.class.getName(), "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethodsTypeUse.class.getName(), "test", "()V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethodsTypeUse.class.getName(), "methodWithExperimentalParameter", "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethodsTypeUse.class.getName(), "methodWithExperimentalTypeParameter", "(Ljava/util/List;)V")));
        Assert.assertTrue(set.contains(new AnnotatedMethod(InterfaceWithExperimentalMethodsTypeUse.class.getName(), "methodWithExperimentalTypeReturn", "()Ljava/util/List;")));
    }

    @Test
    public void testScanConstructorAnnotations() throws Exception {
        File file = TestUtils.createJar(ClassWithExperimentalConstructors.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedConstructor> set = result.getAnnotatedConstructors();
        Assert.assertEquals(3, set.size());

        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "()V")));
        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "(I)V")));
    }

    @Test
    public void testScanConstructorAnnotationsWithTypeUse() throws Exception {
        // The Jandex lookup works differently when TYPE_USE is one of the targets
        File file = TestUtils.createJar(ClassWithExperimentalConstructorsTypeUse.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION_WITH_TYPE_USE, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedConstructor> set = result.getAnnotatedConstructors();
        Assert.assertEquals(4, set.size());

        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructorsTypeUse.class.getName(), "(Ljava/lang/String;)V")));
        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructorsTypeUse.class.getName(), "()V")));
        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructorsTypeUse.class.getName(), "(Ljava/util/List;)V")));
        Assert.assertTrue(set.contains(new AnnotatedConstructor(ClassWithExperimentalConstructorsTypeUse.class.getName(), "(Ljava/util/List;)V")));
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

    @Test
    public void testScanFieldAnnotationsWithTypeUse() throws Exception {
        // The Jandex lookup works differently when TYPE_USE is one of the targets
        File file = TestUtils.createJar(ClassWithExperimentalFieldsTypeUse.class);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION_WITH_TYPE_USE, Collections.emptySet());
        JarAnnotationIndex result = indexer.scanForAnnotation();
        Set<AnnotatedField> set = result.getAnnotatedFields();
        Assert.assertEquals(3, set.size());
        Assert.assertTrue(set.contains(new AnnotatedField(ClassWithExperimentalFieldsTypeUse.class.getName(), "fieldA")));
        Assert.assertTrue(set.contains(new AnnotatedField(ClassWithExperimentalFieldsTypeUse.class.getName(), "fieldB")));
        Assert.assertTrue(set.contains(new AnnotatedField(ClassWithExperimentalFieldsTypeUse.class.getName(), "fieldWithTypeAnnotation")));
    }


    private void checkSet(Set<String> set, String... expected) {
        Assert.assertEquals(expected.length, set.size());
        for (String s : expected) {
            Assert.assertTrue(set.contains(s));
        }
    }
}
