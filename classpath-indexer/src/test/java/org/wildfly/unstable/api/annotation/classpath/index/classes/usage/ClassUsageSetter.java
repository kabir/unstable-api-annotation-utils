package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimental;

public class ClassUsageSetter {
    ClassWithExperimental field;
    public void setter(ClassWithExperimental classWithExperimental) {
        field = classWithExperimental;
    }
}
