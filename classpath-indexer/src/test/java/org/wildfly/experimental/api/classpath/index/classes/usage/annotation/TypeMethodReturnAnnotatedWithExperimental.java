package org.wildfly.experimental.api.classpath.index.classes.usage.annotation;

import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;

import java.util.List;

public class TypeMethodReturnAnnotatedWithExperimental {

    List<@AnnotationWithExperimental String> getList() {
        return null;
    }
}
