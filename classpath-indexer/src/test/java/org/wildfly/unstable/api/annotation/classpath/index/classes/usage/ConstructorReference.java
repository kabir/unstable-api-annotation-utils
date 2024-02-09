package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalConstructors;

public class ConstructorReference {
    public void test() {
        ClassWithExperimentalConstructors clazz = new ClassWithExperimentalConstructors("hi");
        clazz = new ClassWithExperimentalConstructors(1L);
    }
}
