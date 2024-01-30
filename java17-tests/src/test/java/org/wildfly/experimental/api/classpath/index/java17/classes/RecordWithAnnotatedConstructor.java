package org.wildfly.experimental.api.classpath.index.java17.classes;

public record RecordWithAnnotatedConstructor(int i) {
    @Experimental
    public RecordWithAnnotatedConstructor {
    }
}
