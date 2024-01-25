package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Set;

public abstract class DirectAnnotationUsage extends AnnotationUsage {
    public DirectAnnotationUsage(Set<String> annotations, AnnotationUsageType type) {
        super(annotations, type);
    }
}
