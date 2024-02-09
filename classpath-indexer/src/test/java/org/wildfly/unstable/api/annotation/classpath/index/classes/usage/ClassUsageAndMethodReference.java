package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimental;
import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalMethods;

public class ClassUsageAndMethodReference {
    ClassWithExperimentalMethods methodClass;
    public void test(ClassWithExperimental classWithExperimental) {
        classWithExperimental.test();
        methodClass.test();
    }
}
