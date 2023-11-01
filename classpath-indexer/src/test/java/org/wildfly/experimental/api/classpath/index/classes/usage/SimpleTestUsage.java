package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimentalMethods;

import java.util.function.Consumer;

public class SimpleTestUsage extends ClassWithExperimental implements InterfaceWithExperimental {
    InterfaceWithExperimentalMethods iface;
    AnnotationWithExperimentalMethods ann;
    public void test() {
        ClassWithExperimentalMethods cem = new ClassWithExperimentalMethods();
        cem.test();
        ClassWithExperimentalMethods.test("x");

        iface.test();

        ann.value();
    }
}
