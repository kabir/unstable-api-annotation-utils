package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedConstructor;

public class ConstructorReference {
    public void test() {
        RecordWithAnnotatedConstructor r = new RecordWithAnnotatedConstructor(1);
    }
}
