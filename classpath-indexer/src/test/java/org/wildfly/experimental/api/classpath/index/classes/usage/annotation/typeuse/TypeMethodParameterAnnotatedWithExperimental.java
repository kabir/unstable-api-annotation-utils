package org.wildfly.experimental.api.classpath.index.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.classes.TypeUseAnnotationWithExperimental;

import java.util.List;

public class TypeMethodParameterAnnotatedWithExperimental {

    void setList(List<@TypeUseAnnotationWithExperimental String> list) {
    }
}
