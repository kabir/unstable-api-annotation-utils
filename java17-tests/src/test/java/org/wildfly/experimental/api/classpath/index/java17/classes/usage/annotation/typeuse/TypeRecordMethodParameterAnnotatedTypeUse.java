package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimentalTypeUse;

import java.util.List;

public record TypeRecordMethodParameterAnnotatedTypeUse() {

    public void test(List<@AnnotationWithExperimentalTypeUse String> s) {

    }
}
