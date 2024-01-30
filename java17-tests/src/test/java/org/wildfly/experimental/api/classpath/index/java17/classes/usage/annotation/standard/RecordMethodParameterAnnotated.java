package org.wildfly.experimental.api.classpath.index.java17.classes.usage.annotation.standard;

import org.wildfly.experimental.api.classpath.index.java17.classes.AnnotationWithExperimental;

public record RecordMethodParameterAnnotated() {

    public void test(@AnnotationWithExperimental String s) {

    }
}
