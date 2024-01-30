package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedGetterFromConstructorParameter;

public class GetterWithAnnotationFromConstructorUsage {
    RecordWithAnnotatedGetterFromConstructorParameter r;
    public void test() {
        int i = r.i();
    }

}
