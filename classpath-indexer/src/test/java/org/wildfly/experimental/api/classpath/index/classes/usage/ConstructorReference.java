package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalConstructors;

public class ConstructorReference {
    public void test() {
        ClassWithExperimentalConstructors clazz = new ClassWithExperimentalConstructors("hi");
        clazz = new ClassWithExperimentalConstructors(1L);
    }
}
