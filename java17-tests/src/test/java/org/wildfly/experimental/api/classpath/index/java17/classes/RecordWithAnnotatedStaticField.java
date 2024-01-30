package org.wildfly.experimental.api.classpath.index.java17.classes;

public record RecordWithAnnotatedStaticField(int i) {
    @Experimental
    public static String staticField;

    // Instance fields are not allowed in records, so we only have a static field
    // RecordWithAnnotatedConstructor has an annotation on the constructor parameter which is kind of the field
}
