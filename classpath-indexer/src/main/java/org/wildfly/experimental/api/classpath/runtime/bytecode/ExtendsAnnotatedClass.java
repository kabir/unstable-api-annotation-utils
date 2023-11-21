package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.EXTENDS_CLASS;

public class ExtendsAnnotatedClass extends AnnotationWithSourceClassUsage {
    private final String superClass;

    ExtendsAnnotatedClass(Set<String> annotations, String clazz, String superClass) {
        super(annotations, EXTENDS_CLASS, clazz);
        this.superClass = superClass;
    }

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
                convertClassNameToDotFormat(sourceClass),
                convertClassNameToDotFormat(superClass));
    }
}
