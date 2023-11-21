package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.CLASS_USAGE;

public class AnnotatedClassUsage extends AnnotationWithSourceClassUsage {
    private final String referencedClass;

    AnnotatedClassUsage(Set<String> annotations, String className, String referencedClass) {
        super(annotations, CLASS_USAGE, className);
        this.referencedClass = referencedClass;
    }

    public String getReferencedClass() {
        return referencedClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnnotatedClassUsage usage = (AnnotatedClassUsage) o;
        return Objects.equals(referencedClass, usage.referencedClass);
    }

    @Override
    // Don't override hashcode here, the root class will do some caching
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), referencedClass);
    }

    @Override
    protected AnnotationUsage convertToDotFormat() {
        return new AnnotatedClassUsage(
                annotations,
                convertClassNameToDotFormat(sourceClass),
                convertClassNameToDotFormat(referencedClass));
    }
}
