package org.wildfly.experimental.api.classpath.index.classes.usage.annotation;

import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;

import java.util.ArrayList;
import java.util.List;

public class TypeMethodBodyAnnotatedWithExperimental {

    void createList() {
        List<@AnnotationWithExperimental String> l = new ArrayList<>();
    }
}
