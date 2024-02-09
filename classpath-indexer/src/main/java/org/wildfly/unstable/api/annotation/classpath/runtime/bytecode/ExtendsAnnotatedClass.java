package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;

import org.wildfly.unstable.api.annotation.classpath.index.OverallIndex;
import org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex;

import java.util.Objects;
import java.util.Set;

/**
 * Records a class in user bytecode that extends a class that has been annotated with one of the annotations
 * we recorded as unstable api in the {@link OverallIndex}
 */
public class ExtendsAnnotatedClass extends AnnotationWithSourceClassUsage {
    private final String superClass;

    ExtendsAnnotatedClass(Set<String> annotations, String clazz, String superClass) {
        super(annotations, AnnotationUsageType.EXTENDS_CLASS, clazz);
        this.superClass = superClass;
    }

    /**
     * Gets the extended class that has been annotated with one of the unstable api annotations
     * @return the super class name
     */
    public String getSuperClass() {
        return superClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExtendsAnnotatedClass that = (ExtendsAnnotatedClass) o;
        return Objects.equals(superClass, that.superClass);
    }

    @Override
    // Don't override hashcode here, the root class will do some caching
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), superClass);
    }

    @Override
    protected AnnotationUsage convertToDotFormat() {
        return new ExtendsAnnotatedClass(
                annotations,
                RuntimeIndex.convertClassNameToDotFormat(sourceClass),
                RuntimeIndex.convertClassNameToDotFormat(superClass));
    }
}
