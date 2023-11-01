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
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassExtendsUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.NoUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.ExtendsAnnotatedClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.EXTENDS_CLASS;

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
    public void testNoUsage() throws Exception {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = scanClass(inspector, NoUsage.class);
        Assert.assertTrue(ok);
        Assert.assertEquals(0, inspector.getUsages().size());
    }

    @Test
    public void testClassExtendsUsage() throws Exception {
        ClassBytecodeInspector inspector = new ClassBytecodeInspector(runtimeIndex);
        boolean ok = scanClass(inspector, ClassExtendsUsage.class);
        Assert.assertFalse(ok);
        Assert.assertEquals(1, inspector.getUsages().size());
        AnnotationUsage usage = inspector.getUsages().iterator().next();
        Assert.assertEquals(EXTENDS_CLASS, usage.getType());
        ExtendsAnnotatedClass extendsClass = (ExtendsAnnotatedClass) usage;

        Assert.assertEquals(convertClassNameToVmFormat(ClassExtendsUsage.class), extendsClass.getSourceClass());
        Assert.assertEquals(convertClassNameToVmFormat(ClassWithExperimental.class), extendsClass.getSuperClass());
        // TODO is it weird that we are using JVM format for everything else but not here?
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), extendsClass.getAnnotations());
    }


    String convertClassNameToVmFormat(Class<?> clazz) {
        return RuntimeIndex.convertClassNameToVmFormat(clazz.getName());
    }

    private boolean scanClass(ClassBytecodeInspector inspector, Class<?> clazz) throws IOException {
        String classLocation = clazz.getName().replaceAll("\\.", "/") + ".class";
        URL url = RuntimeTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            return inspector.scanClassFile(clazz.getName(), in);
        }
    }
}
