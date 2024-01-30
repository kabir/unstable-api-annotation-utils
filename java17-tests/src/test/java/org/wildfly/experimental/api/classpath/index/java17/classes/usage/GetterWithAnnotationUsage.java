package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedGetter;

public class GetterWithAnnotationUsage {
    RecordWithAnnotatedGetter r;
    public void test() {
        int i = r.i();
    }

}
