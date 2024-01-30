package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimentalTypeUse;

import java.util.List;

public record TypeRecordConstructorCanonicalParameterAnnotatedTypeUse(List<String> s) {
    public TypeRecordConstructorCanonicalParameterAnnotatedTypeUse(List<@AnnotationWithExperimentalTypeUse String> s) {
        this.s = s;
    }
}
