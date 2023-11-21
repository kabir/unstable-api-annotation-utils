package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

public abstract class AnnotationWithSourceClassUsage extends AnnotationUsage {
    protected final String sourceClass;

    AnnotationWithSourceClassUsage(Set<String> annotations, AnnotationUsageType type, String sourceClass) {
        super(annotations, type);
        this.sourceClass = sourceClass;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnnotationWithSourceClassUsage that = (AnnotationWithSourceClassUsage) o;
        return Objects.equals(sourceClass, that.sourceClass);
    }

    @Override
    // Don't override hashcode here, the root class will do some caching
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), sourceClass);
    }
}
