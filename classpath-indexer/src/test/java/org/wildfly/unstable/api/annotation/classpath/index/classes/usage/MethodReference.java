package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalMethods;

public class MethodReference {
    public void test() {
        ClassWithExperimentalMethods clazz = new ClassWithExperimentalMethods();
        clazz.test();
        clazz.notAnnotated();
    }
}
