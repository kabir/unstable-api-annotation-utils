package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimentalTypeUse;

public record RecordConstructorCanonicalParameterAnnotatedTypeUse(int i) {
    public RecordConstructorCanonicalParameterAnnotatedTypeUse(@AnnotationWithExperimentalTypeUse int i) {
        this.i = i;
    }
}
