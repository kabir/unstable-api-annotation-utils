package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;

public class ClassUsageInMethodBody {
    public void test(ClassWithExperimental classWithExperimental) {
        classWithExperimental.test();
    }
}
