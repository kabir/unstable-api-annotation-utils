package org.wildfly.experimental.api.classpath.index;

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
import org.wildfly.experimental.api.classpath.index.classes.usage.NoUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class RuntimeTestCase {
    private static final String EXPERIMENTAL_ANNOTATION = Experimental.class.getName();
    RuntimeIndex runtimeIndex;
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

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testSimpleTestUsage() throws Exception {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = scanClass(inspector, NoUsage.class);
        Assert.assertTrue(ok);
        Assert.assertEquals(0, inspector.getUsages().size());
    }



    private boolean scanClass(ClassBytecodeInspector inspector, Class<?> clazz) throws IOException {
        String classLocation = clazz.getName().replaceAll("\\.", "/") + ".class";
        URL url = RuntimeTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            return inspector.scanClassFile(clazz.getName(), in);
        }
    }
}
