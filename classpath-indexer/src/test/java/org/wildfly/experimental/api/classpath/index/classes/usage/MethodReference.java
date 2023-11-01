package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;

public class MethodReference {
    public void test() {
        ClassWithExperimentalMethods clazz = new ClassWithExperimentalMethods();
        clazz.test();
        clazz.notAnnotated();
    }
}
