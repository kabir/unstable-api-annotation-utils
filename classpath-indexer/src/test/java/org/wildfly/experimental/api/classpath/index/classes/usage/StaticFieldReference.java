package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalFields;

public class StaticFieldReference {

    public void test() {
        String s = ClassWithExperimentalFields.fieldB;
    }
}
