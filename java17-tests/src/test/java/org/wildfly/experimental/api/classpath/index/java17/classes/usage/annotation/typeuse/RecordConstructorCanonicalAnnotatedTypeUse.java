package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimentalTypeUse;

public record RecordConstructorCanonicalAnnotatedTypeUse() {
    @AnnotationWithExperimentalTypeUse
    public RecordConstructorCanonicalAnnotatedTypeUse() {
    }
}
