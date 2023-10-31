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

    @Test
    public void testOverAllIndex() throws Exception {
        OverallIndex overallIndex = createOverallIndexWithEverything();

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
        OverallIndex index = createOverallIndexWithEverything();
        Path path = Paths.get("target/index/index.txt");
        index.save(path);

        OverallIndex loaded = OverallIndex.load(path);
        Assert.assertEquals(index, loaded);
    }

    private OverallIndex createOverallIndexWithEverything() throws IOException {
        OverallIndex overallIndex = new OverallIndex();
        overallIndex.mergeAnnotationIndex(createJarIndex(AnnotationWithExperimental.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(ClassWithExperimental.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(InterfaceWithExperimental.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(ClassWithExperimentalMethods.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(InterfaceWithExperimentalMethods.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(AnnotationWithExperimentalMethods.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(ClassWithExperimentalConstructors.class));
        overallIndex.mergeAnnotationIndex(createJarIndex(ClassWithExperimentalFields.class));
        return overallIndex;
    }

    private AnnotationIndex createJarIndex(Class<?>... classes) throws IOException {
        File file = TestUtils.createJar(classes);
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(file, EXPERIMENTAL_ANNOTATION, Collections.emptySet());
        return indexer.scanForAnnotation();
    }


    private void checkSet(Set<String> set, String... expected) {
        Assert.assertEquals(expected.length, set.size());
        for (String s : expected) {
            Assert.assertTrue(set.contains(s));
        }
    }
}
