package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedMethod;

public class MethodReference {
    RecordWithAnnotatedMethod r;
    public void test() {
        r.method();
    }
}
