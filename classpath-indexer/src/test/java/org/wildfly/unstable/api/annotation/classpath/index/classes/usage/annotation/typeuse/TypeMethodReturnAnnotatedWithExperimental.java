package org.wildfly.unstable.api.annotation.classpath.index.classes.usage.annotation.typeuse;

import org.wildfly.unstable.api.annotation.classpath.index.classes.TypeUseAnnotationWithExperimental;

import java.util.List;

public class TypeMethodReturnAnnotatedWithExperimental {

    List<@TypeUseAnnotationWithExperimental String> getList() {
        return null;
    }
}
