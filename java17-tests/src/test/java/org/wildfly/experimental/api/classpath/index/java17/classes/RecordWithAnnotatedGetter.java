package org.wildfly.experimental.api.classpath.index.java17.classes;

public record RecordWithAnnotatedGetter(int i) {

    @Experimental
    public int i() {
        return i;
    }
}
