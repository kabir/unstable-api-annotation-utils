package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimentalMethods;

public class SimpleTestUsage {
    InterfaceWithExperimentalMethods iface;
    public void test() {
        ClassWithExperimentalMethods cem = new ClassWithExperimentalMethods();
        cem.test();
        ClassWithExperimentalMethods.test("x");

        iface.test();
    }
}
