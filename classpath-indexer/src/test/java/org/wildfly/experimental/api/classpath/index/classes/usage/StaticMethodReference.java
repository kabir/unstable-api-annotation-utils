package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;

public class StaticMethodReference {
    public void test() {
        ClassWithExperimentalMethods.test("x");
    }
}
