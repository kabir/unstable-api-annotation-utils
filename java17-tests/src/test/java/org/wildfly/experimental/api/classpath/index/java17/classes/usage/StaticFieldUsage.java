package org.wildfly.experimental.api.classpath.index.java17.classes.usage;

import org.wildfly.experimental.api.classpath.index.java17.classes.RecordWithAnnotatedStaticField;

public class StaticFieldUsage {
    public void test() {
        // Instance fields are not allowed in records, so we only have a static field
        RecordWithAnnotatedStaticField.staticField = "Hi";
    }

}
