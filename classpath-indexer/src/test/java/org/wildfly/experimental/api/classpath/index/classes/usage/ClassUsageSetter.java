package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;

public class ClassUsageSetter {
    ClassWithExperimental field;
    public void setter(ClassWithExperimental classWithExperimental) {
        field = classWithExperimental;
    }
}
