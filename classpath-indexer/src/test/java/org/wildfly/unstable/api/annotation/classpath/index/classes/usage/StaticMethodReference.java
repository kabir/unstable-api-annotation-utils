package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalMethods;

public class StaticMethodReference {
    public void test() {
        ClassWithExperimentalMethods.test("x");
    }
}
