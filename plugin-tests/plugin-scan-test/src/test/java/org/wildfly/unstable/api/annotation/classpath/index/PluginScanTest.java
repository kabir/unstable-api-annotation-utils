package org.wildfly.unstable.api.annotation.classpath.index;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.unstable.api.annotation.test.api.MarkerAnnotation;
import org.wildfly.unstable.api.annotation.test.api.OtherMarker;
import org.wildfly.unstable.api.annotation.test.api.a.ClassWithAnnotationA;
import org.wildfly.unstable.api.annotation.test.api.a.ClassWithOtherAnnotation;
import org.wildfly.unstable.api.annotation.test.api.b.ClassWithAnnotationB;
import org.wildfly.unstable.api.annotation.test.api.b.MethodWithAnnotation;
import org.wildfly.unstable.api.annotation.test.api.unscanned.UnscannedClass;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

/**
 * The pom is set up to use the plugin to scan the classpath for use of annotations.
 * This test verifies the generated index is valid
 */
public class PluginScanTest {
    @Test
    public void checkCreatedIndex() throws Exception {
        // Make sure that this class is on the classpath. The scanner is set
        // up to not scan it, so it should not show up in the results
        Class<?> unscanned = UnscannedClass.class;


        Path p = Paths.get("target/index/test-scan-index.txt");

        OverallIndex overall = OverallIndex.load(p);
        Set<String> annotations = overall.getAnnotations();
        Assert.assertEquals(2, annotations.size());

        Assert.assertTrue(annotations.contains(MarkerAnnotation.class.getName()));
        Assert.assertTrue(annotations.contains(OtherMarker.class.getName()));

        // Checks for @MarkerAnnotation
        AnnotationIndex index = overall.getAnnotationIndex(MarkerAnnotation.class.getName());

        Set<String> classes = index.getAnnotatedClasses();
        Assert.assertEquals(2, classes.size());
        Assert.assertTrue(classes.contains(ClassWithAnnotationA.class.getName()));
        Assert.assertTrue(classes.contains(ClassWithAnnotationB.class.getName()));

        Set<AnnotatedMethod> methods = index.getAnnotatedMethods();
        Assert.assertEquals(1, methods.size());
        AnnotatedMethod method = new AnnotatedMethod(MethodWithAnnotation.class.getName(), "testMethod", "()V");
        Assert.assertTrue(methods.contains(method));

        assertEmpty(index.getAnnotatedAnnotations());
        assertEmpty(index.getAnnotatedConstructors());
        assertEmpty(index.getAnnotatedFields());
        assertEmpty(index.getAnnotatedInterfaces());

        // Checks for @OtherAnnotation
        index = overall.getAnnotationIndex(OtherMarker.class.getName());

        classes = index.getAnnotatedClasses();
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(classes.contains(ClassWithOtherAnnotation.class.getName()));

        assertEmpty(index.getAnnotatedAnnotations());
        assertEmpty(index.getAnnotatedConstructors());
        assertEmpty(index.getAnnotatedFields());
        assertEmpty(index.getAnnotatedInterfaces());
        assertEmpty(index.getAnnotatedMethods());



    }

    private void assertEmpty(Collection<?> collection) {
        Assert.assertTrue(collection.isEmpty());
    }
}
