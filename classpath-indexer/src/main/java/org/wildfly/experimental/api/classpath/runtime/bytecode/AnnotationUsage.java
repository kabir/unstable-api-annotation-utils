package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATION_USAGE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.CLASS_USAGE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.EXTENDS_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.IMPLEMENTS_INTERFACE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

public abstract class AnnotationUsage {
    protected final Set<String> annotations;
    protected final AnnotationUsageType type;

    private int hash;

    AnnotationUsage(Set<String> annotations, AnnotationUsageType type) {
        this.annotations = annotations;
        this.type = type;
    }

    public AnnotationUsageType getType() {
        return type;
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    public ExtendsAnnotatedClass asExtendsAnnotatedClass() {
        if (type != EXTENDS_CLASS) {
            throw new IllegalStateException();
        }
        return (ExtendsAnnotatedClass) this;
    }

    public ImplementsAnnotatedInterface asImplementsAnnotatedInterface() {
        if (type != IMPLEMENTS_INTERFACE) {
            throw new IllegalStateException();
        }
        return (ImplementsAnnotatedInterface) this;
    }

    public AnnotatedMethodReference asAnnotatedMethodReference() {
        if (type != METHOD_REFERENCE) {
            throw new IllegalStateException();
        }
        return (AnnotatedMethodReference) this;
    }

    public AnnotatedFieldReference asAnnotatedFieldReference() {
        if (type != FIELD_REFERENCE) {
            throw new IllegalStateException();
        }
        return (AnnotatedFieldReference) this;
    }

    public AnnotatedClassUsage asAnnotatedClassUsage() {
        if (type != CLASS_USAGE) {
            throw new IllegalStateException();
        }
        return (AnnotatedClassUsage) this;
    }

    public AnnotatedAnnotation asAnnotatedAnnotation() {
        if (type != ANNOTATION_USAGE) {
            throw new IllegalStateException();
        }
        return (AnnotatedAnnotation) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationUsage usage = (AnnotationUsage) o;
        return Objects.equals(annotations, usage.annotations) && type == usage.type;
    }

    @Override
    public final int hashCode() {
        int h = hash;
        if (hash == 0) {
            h = calculateHash();
            hash = h;
        }
        return h;
    }

    protected int calculateHash() {
        return Objects.hash(annotations, type);
    }

    // When reading the bytecode, the class names will be in JVM format (e.g. java/lang/Class).
    // We keep that for fast lookups during the indexing.
    // When returning the data to the users we convert to the more familiar 'dot format' (i.e. java.lang.Class)
    protected abstract AnnotationUsage convertToDotFormat();
}
