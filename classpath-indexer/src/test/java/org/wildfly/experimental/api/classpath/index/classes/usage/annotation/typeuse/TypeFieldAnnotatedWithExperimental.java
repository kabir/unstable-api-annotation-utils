package org.wildfly.experimental.api.classpath.index.classes.usage.annotation.typeuse;

import org.wildfly.experimental.api.classpath.index.classes.TypeUseAnnotationWithExperimental;

import java.util.List;

public class TypeFieldAnnotatedWithExperimental {

    List<@TypeUseAnnotationWithExperimental String> typeField;
}
