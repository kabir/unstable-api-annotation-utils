package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedMethodParameter;

public class MethodWithParameterReference {
    RecordWithAnnotatedMethodParameter r;
    public void test() {
        r.method("Hi");
    }
}
