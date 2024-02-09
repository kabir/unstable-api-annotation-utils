package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimentalFields;

public class StaticFieldReference {

    public void test() {
        String s = ClassWithExperimentalFields.fieldB;
    }
}
