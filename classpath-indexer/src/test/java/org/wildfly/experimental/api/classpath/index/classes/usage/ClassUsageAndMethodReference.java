package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;

public class ClassUsageAndMethodReference {
    ClassWithExperimentalMethods methodClass;
    public void test(ClassWithExperimental classWithExperimental) {
        classWithExperimental.test();
        methodClass.test();
    }
}
