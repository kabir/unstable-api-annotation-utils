package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Set;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATION_USAGE;

public class AnnotatedAnnotation extends AnnotationUsage {
    AnnotatedAnnotation(Set<String> annotations) {
        super(annotations, ANNOTATION_USAGE);
    }

    @Override
    protected AnnotationUsage convertToDotFormat() {
        return new AnnotatedAnnotation(annotations);
    }
}
