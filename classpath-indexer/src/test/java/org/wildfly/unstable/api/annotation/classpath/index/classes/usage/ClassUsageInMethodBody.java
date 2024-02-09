package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimental;

public class ClassUsageInMethodBody {
    public void test(ClassWithExperimental classWithExperimental) {
        classWithExperimental.test();
    }
}
