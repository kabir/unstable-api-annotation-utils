package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalFields;

public class FieldReference {
    ClassWithExperimentalFields cl;

    public void test() {
        String s = cl.fieldA;
    }
}
