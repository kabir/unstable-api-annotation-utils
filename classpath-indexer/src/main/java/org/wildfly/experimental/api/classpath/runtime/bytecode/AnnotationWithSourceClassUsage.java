package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

/**
 * Abstract base class for all usage of classes, methods, fields, constructors etc. that have been annotated with
 * annotations marked as 'experimental', where there is a class in the user source code that contains one of these
 * references. The intent is for this to be run on code supplied by the user.
 */
public abstract class AnnotationWithSourceClassUsage extends AnnotationUsage {
    protected final String sourceClass;

    AnnotationWithSourceClassUsage(Set<String> annotations, AnnotationUsageType type, String sourceClass) {
        super(annotations, type);
        this.sourceClass = sourceClass;
    }

    /**
     * Gets the class containing the annotation usage
     * @return the class name
     */
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
