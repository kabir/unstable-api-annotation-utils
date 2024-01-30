package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimentalTypeUse;

public record RecordGetterAnnotatedTypeUse(int i) {
    @AnnotationWithExperimentalTypeUse
    public int i() {
        return i;
    }
}
