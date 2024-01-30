package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimental;

public record RecordConstructorCanonicalParameterAnnotated(int i) {
    public RecordConstructorCanonicalParameterAnnotated(@AnnotationWithExperimental int i) {
        this.i = i;
    }
}
