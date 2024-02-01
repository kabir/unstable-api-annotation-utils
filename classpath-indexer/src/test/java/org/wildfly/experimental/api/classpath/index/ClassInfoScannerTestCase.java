package org.wildfly.experimental.api.classpath.index;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassArrayUsageAsField;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassArrayUsageAsMethodParameter;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassArrayUsageAsMethodReturnType;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassArrayUsageInMethodBody;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassExtendsUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassImplementsUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassUsageAndMethodReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassUsageAsField;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassUsageAsMethodParameter;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassUsageAsMethodReturnType;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassUsageInMethodBody;
import org.wildfly.experimental.api.classpath.index.classes.usage.ClassUsageSetter;
import org.wildfly.experimental.api.classpath.index.classes.usage.ConstructorReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.FieldReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.MethodReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.NoUsage;
import org.wildfly.experimental.api.classpath.index.classes.usage.StaticFieldReference;
import org.wildfly.experimental.api.classpath.index.classes.usage.StaticMethodReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotatedClassUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotatedFieldReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotatedMethodReference;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassInfoScanner;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ExtendsAnnotatedClass;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ImplementsAnnotatedInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.CLASS_USAGE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.EXTENDS_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.IMPLEMENTS_INTERFACE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

public class ClassInfoScannerTestCase {
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
        overallIndex.scanJar(file, EXPERIMENTAL_ANNOTATION);

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        runtimeIndex = RuntimeIndex.load(p);
    }

    @Test
    public void testNoUsage() throws Exception {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, NoUsage.class);
        Assert.assertEquals(0, scanner.getUsages().size());
    }

    @Test
    public void testClassExtendsUsage() throws Exception {
        ExtendsAnnotatedClass usage =
                scanAndGetSingleAnnotationUsage(ClassExtendsUsage.class, EXTENDS_CLASS)
                        .asExtendsAnnotatedClass();

        Assert.assertEquals(ClassExtendsUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), usage.getSuperClass());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testClassImplementsUsage() throws Exception {
        ImplementsAnnotatedInterface usage =
                scanAndGetSingleAnnotationUsage(ClassImplementsUsage.class, IMPLEMENTS_INTERFACE)
                        .asImplementsAnnotatedInterface();

        Assert.assertEquals(ClassImplementsUsage.class.getName(), usage.getSourceClass());
        Assert.assertEquals(InterfaceWithExperimental.class.getName(), usage.getInterface());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testConstructorReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(ConstructorReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(ConstructorReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalConstructors.class.getName(), usage.getMethodClass());
        Assert.assertEquals(RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME, usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testFieldReference() throws Exception {
        AnnotatedFieldReference usage =
                scanAndGetSingleAnnotationUsage(FieldReference.class, FIELD_REFERENCE)
                        .asAnnotatedFieldReference();

        Assert.assertEquals(FieldReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalFields.class.getName(), usage.getFieldClass());
        Assert.assertEquals("fieldA", usage.getFieldName());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testStaticFieldReference() throws Exception {
        AnnotatedFieldReference usage =
                scanAndGetSingleAnnotationUsage(StaticFieldReference.class, FIELD_REFERENCE)
                        .asAnnotatedFieldReference();

        Assert.assertEquals(StaticFieldReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalFields.class.getName(), usage.getFieldClass());
        Assert.assertEquals("fieldB", usage.getFieldName());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testMethodReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(MethodReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(MethodReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalMethods.class.getName(), usage.getMethodClass());
        Assert.assertEquals("test", usage.getMethodName());
        Assert.assertEquals("()V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    @Test
    public void testStaticMethodReference() throws Exception {
        AnnotatedMethodReference usage =
                scanAndGetSingleAnnotationUsage(StaticMethodReference.class, METHOD_REFERENCE)
                        .asAnnotatedMethodReference();

        Assert.assertEquals(StaticMethodReference.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalMethods.class.getName(), usage.getMethodClass());
        Assert.assertEquals("test", usage.getMethodName());
        Assert.assertEquals("(Ljava/lang/String;)V", usage.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), usage.getAnnotations());
    }

    //TODO If this turns out to be important, we might want to look at the UTF8Infos where the classname appears
    // If it is a match, temporarily record it. If the not registered by the normal means then we will need to search fields + methods
    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassUsageAsField() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassUsageAsField.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassUsageAsField.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassArrayUsageAsField() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassArrayUsageAsField.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassArrayUsageAsField.class.getName(), usage.getSourceClass());
        Assert.assertEquals(InterfaceWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassUsageAsMethodParameter() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassUsageAsMethodParameter.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassUsageAsMethodParameter.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassArrayUsageAsMethodParameter() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassArrayUsageAsMethodParameter.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassArrayUsageAsMethodParameter.class.getName(), usage.getSourceClass());
        Assert.assertEquals(InterfaceWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassUsageAsMethodReturnType() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassUsageAsMethodReturnType.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassUsageAsMethodReturnType.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassArrayUsageAsMethodReturnType() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassArrayUsageAsMethodReturnType.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassArrayUsageAsMethodReturnType.class.getName(), usage.getSourceClass());
        Assert.assertEquals(InterfaceWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    @Ignore("Just referencing a class in a declaration doesn't seem to add it unless it is actually used, as in testClassUsageAsMethodBody()")
    public void testClassUsageSetter() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassUsageSetter.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassUsageSetter.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    public void testClassUsageAsMethodBody() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassUsageInMethodBody.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassUsageInMethodBody.class.getName(), usage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    public void testClassArrayUsageAsMethodBody() throws Exception {
        AnnotatedClassUsage usage =
                scanAndGetSingleAnnotationUsage(ClassArrayUsageInMethodBody.class, CLASS_USAGE)
                        .asAnnotatedClassUsage();
        Assert.assertEquals(ClassArrayUsageInMethodBody.class.getName(), usage.getSourceClass());
        Assert.assertEquals(InterfaceWithExperimental.class.getName(), usage.getReferencedClass());
    }

    @Test
    public void testClassUsageAndMethodReference() throws Exception {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, ClassUsageAndMethodReference.class);
        Assert.assertEquals(2, scanner.getUsages().size());
        Map<AnnotationUsageType, AnnotationUsage> usages = new HashMap<>();
        for (AnnotationUsage usage : scanner.getUsages()) {
            if (usage.getType() == CLASS_USAGE) {
                usages.put(CLASS_USAGE, usage);
            } else if (usage.getType() == METHOD_REFERENCE) {
                usages.put(METHOD_REFERENCE, usage);
            } else {
                Assert.fail("Unexpected type");
            }
        }

        AnnotatedClassUsage classUsage = usages.get(CLASS_USAGE).asAnnotatedClassUsage();
        Assert.assertEquals(ClassUsageAndMethodReference.class.getName(), classUsage.getSourceClass());
        Assert.assertEquals(ClassWithExperimental.class.getName(), classUsage.getReferencedClass());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), classUsage.getAnnotations());

        AnnotatedMethodReference methodReference = usages.get(METHOD_REFERENCE).asAnnotatedMethodReference();
        Assert.assertEquals(ClassUsageAndMethodReference.class.getName(), methodReference.getSourceClass());
        Assert.assertEquals(ClassWithExperimentalMethods.class.getName(), methodReference.getMethodClass());
        Assert.assertEquals("test", methodReference.getMethodName());
        Assert.assertEquals("()V", methodReference.getDescriptor());
        Assert.assertEquals(Collections.singleton(Experimental.class.getName()), methodReference.getAnnotations());
    }

    AnnotationUsage scanAndGetSingleAnnotationUsage(
            Class<?> clazz,
            AnnotationUsageType type) throws IOException {
        ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);
        scanClass(scanner, clazz);

        Assert.assertEquals(1, scanner.getUsages().size());
        AnnotationUsage usage = scanner.getUsages().iterator().next();
        Assert.assertEquals(type, usage.getType());
        return usage;
    }

    private void scanClass(ClassInfoScanner scanner, Class<?> clazz) throws IOException {
        String classLocation = clazz.getName().replaceAll("\\.", "/") + ".class";
        URL url = ClassInfoScannerTestCase.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            scanner.scanClass(in);
        }
    }

}
