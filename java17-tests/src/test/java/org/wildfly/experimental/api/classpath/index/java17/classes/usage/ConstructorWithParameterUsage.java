package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedConstructorParameter;

public class ConstructorWithParameterUsage {
    public void test() {
        RecordWithAnnotatedConstructorParameter r = new RecordWithAnnotatedConstructorParameter(1);
    }
}
