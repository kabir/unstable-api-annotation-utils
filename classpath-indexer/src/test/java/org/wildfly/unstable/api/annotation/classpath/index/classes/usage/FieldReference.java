package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalFields;

public class FieldReference {
    ClassWithExperimentalFields cl;

    public void test() {
        String s = cl.fieldA;
    }
}
