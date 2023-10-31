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
import org.wildfly.experimental.api.classpath.index.classes.Incubating;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithIncubating;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.AnnotatedMethod.ClassType.ANNOTATION;
import static org.wildfly.experimental.api.classpath.index.AnnotatedMethod.ClassType.CLASS;
import static org.wildfly.experimental.api.classpath.index.AnnotatedMethod.ClassType.INTERFACE;

public class OverallIndexTestCase {

    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    private static final String INCUBATING_ANNOTATION = Incubating.class.getName();

    @Test
    public void testOverAllIndexOneAnnotation() throws Exception {
        OverallIndex overallIndex = createOverallIndexWithEverythingExperimental();

        Assert.assertEquals(1, overallIndex.getAnnotations().size());
        Assert.assertTrue(overallIndex.getAnnotations().contains(EXPERIMENTAL_ANNOTATION));

        AnnotationIndex index = overallIndex.getAnnotationIndex(EXPERIMENTAL_ANNOTATION);
        checkSet(index.getAnnotatedAnnotations(), AnnotationWithExperimental.class.getName());
        checkSet(index.getAnnotatedClasses(), ClassWithExperimental.class.getName());
        checkSet(index.getAnnotatedInterfaces(), InterfaceWithExperimental.class.getName());

        Set<AnnotatedMethod> methodsSet = index.getAnnotatedMethods();
        Assert.assertEquals(5, methodsSet.size());
        Assert.assertTrue(methodsSet.contains(new AnnotatedMethod(AnnotationWithExperimentalMethods.class.getName(), ANNOTATION, "value", "()Ljava/lang/String;")));
        Assert.assertTrue(methodsSet.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), CLASS, "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(methodsSet.contains(new AnnotatedMethod(ClassWithExperimentalMethods.class.getName(), CLASS, "test", "()V")));
        Assert.assertTrue(methodsSet.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), INTERFACE, "test", "(Ljava/lang/String;)V")));
        Assert.assertTrue(methodsSet.contains(new AnnotatedMethod(InterfaceWithExperimentalMethods.class.getName(), INTERFACE, "test", "()V")));

        Set<AnnotatedConstructor> constructorSet = index.getAnnotatedConstructors();
        Assert.assertEquals(2, constructorSet.size());
        Assert.assertTrue(constructorSet.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "(Ljava/lang/String;)V")));
        Assert.assertTrue(constructorSet.contains(new AnnotatedConstructor(ClassWithExperimentalConstructors.class.getName(), "()V")));

        Set<AnnotatedField> fieldSet = index.getAnnotatedFields();
        Assert.assertEquals(2, fieldSet.size());
        Assert.assertTrue(fieldSet.contains(new AnnotatedField(ClassWithExperimentalFields.class.getName(), "fieldA")));
        Assert.assertTrue(fieldSet.contains(new AnnotatedField(ClassWithExperimentalFields.class.getName(), "fieldB")));
    }

    @Test
    public void testOverAllIndexWithEverythingSerialization() throws Exception {
        OverallIndex index = createOverallIndexWithEverythingExperimental();
        Path path = Paths.get("target/index/index.txt");
        index.save(path);

        OverallIndex loaded = OverallIndex.load(path);
        Assert.assertEquals(index, loaded);
    }

    @Test
    public void testOverallIndexWithTwoAnnotations() throws Exception {
        OverallIndex overallIndex = createOverallIndexWithEverythingExperimental();
        addJarIndex(INCUBATING_ANNOTATION, overallIndex, InterfaceWithIncubating.class);

        // Check the new annotation
        AnnotationIndex index = overallIndex.getAnnotationIndex(INCUBATING_ANNOTATION);
        checkSet(index.getAnnotatedInterfaces(), InterfaceWithIncubating.class.getName());


        // Sanity test that the other index elements are there too (we've tested this better elsewhere)
        index = overallIndex.getAnnotationIndex(EXPERIMENTAL_ANNOTATION);
        checkSet(index.getAnnotatedInterfaces(), InterfaceWithExperimental.class.getName());

        Path path = Paths.get("target/index/index2.txt");
        overallIndex.save(path);

        OverallIndex loaded = OverallIndex.load(path);
        Assert.assertEquals(overallIndex, loaded);
    }

    private OverallIndex createOverallIndexWithEverythingExperimental() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        addJarIndex(overallIndex, AnnotationWithExperimental.class);
        addJarIndex(overallIndex, ClassWithExperimental.class);
        addJarIndex(overallIndex, InterfaceWithExperimental.class);
        addJarIndex(overallIndex, ClassWithExperimentalMethods.class);
        addJarIndex(overallIndex, InterfaceWithExperimentalMethods.class);
        addJarIndex(overallIndex, AnnotationWithExperimentalMethods.class);
        addJarIndex(overallIndex, ClassWithExperimentalConstructors.class);
        addJarIndex(overallIndex, ClassWithExperimentalFields.class);
        return overallIndex;
    }

    private void addJarIndex(OverallIndex index, Class<?>... classes) throws IOException {
        addJarIndex(EXPERIMENTAL_ANNOTATION, index, classes);
    }

    private void addJarIndex(String annotation, OverallIndex index, Class<?>... classes) throws IOException {
        File file = TestUtils.createJar(classes);
        index.scanJar(file, annotation, Collections.emptySet());
    }


    private void checkSet(Set<String> set, String... expected) {
        Assert.assertEquals(expected.length, set.size());
        for (String s : expected) {
            Assert.assertTrue(set.contains(s));
        }
    }
}
