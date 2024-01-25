package org.wildfly.experimental.api.classpath.index.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.classes.TypeUseAnnotationWithExperimental;

import java.util.ArrayList;
import java.util.List;

public class TypeMethodBodyAnnotatedWithExperimental {

    void createList() {
        List<@TypeUseAnnotationWithExperimental String> l = new ArrayList<>();
    }
}
