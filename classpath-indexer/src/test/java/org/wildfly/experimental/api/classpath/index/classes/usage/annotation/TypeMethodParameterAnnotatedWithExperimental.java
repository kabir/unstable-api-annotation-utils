package org.wildfly.experimental.api.classpath.index.classes.usage.annotation;

import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;

import java.util.List;

public class TypeMethodParameterAnnotatedWithExperimental {

    void setList(List<@AnnotationWithExperimental String> list) {
    }
}
