package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_FIELD;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_METHOD;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.CLASS_USAGE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.EXTENDS_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.IMPLEMENTS_INTERFACE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

/**
 * Abstract base class for all usage of classes, methods, fields, constructors etc. where annotations marked
 * as 'experimental' happened. The intent is for this to be run on code supplied by the user.
 */
public abstract class AnnotationUsage {
    protected final Set<String> annotations;
    protected final AnnotationUsageType type;

    private int hash;

    AnnotationUsage(Set<String> annotations, AnnotationUsageType type) {
        this.annotations = annotations;
        this.type = type;
    }

    /**
     * Gets the type of annotation usage for this instance. The type
     * indicates which of the {@code asXXX()} methods we can call. These methods in turn
     * cast to the indicated type
     * @return the annotation usage type.
     */
    public AnnotationUsageType getType() {
        return type;
    }

    /**
     * Gets the indexed annotations used for this particular usage
     * @return the annotations
     */
    public Set<String> getAnnotations() {
        return annotations;
    }

    /**
     * Casts this instance to {@link ExtendsAnnotatedClass}
     * @return this instance cast to {@link ExtendsAnnotatedClass}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#EXTENDS_CLASS}
     */
    public ExtendsAnnotatedClass asExtendsAnnotatedClass() {
        if (type != EXTENDS_CLASS) {
            throw new IllegalStateException();
        }
        return (ExtendsAnnotatedClass) this;
    }

    /**
     * Casts this instance to {@link ImplementsAnnotatedInterface}
     * @return this instance cast to {@link ImplementsAnnotatedInterface}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#IMPLEMENTS_INTERFACE}
     */
    public ImplementsAnnotatedInterface asImplementsAnnotatedInterface() {
        if (type != IMPLEMENTS_INTERFACE) {
            throw new IllegalStateException();
        }
        return (ImplementsAnnotatedInterface) this;
    }

    /**
     * Casts this instance to {@link AnnotatedMethodReference}
     * @return this instance cast to {@link AnnotatedMethodReference}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#METHOD_REFERENCE}
     */
    public AnnotatedMethodReference asAnnotatedMethodReference() {
        if (type != METHOD_REFERENCE) {
            throw new IllegalStateException();
        }
        return (AnnotatedMethodReference) this;
    }

    /**
     * Casts this instance to {@link AnnotatedFieldReference}
     * @return this instance cast to {@link AnnotatedFieldReference}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#FIELD_REFERENCE}
     */
    public AnnotatedFieldReference asAnnotatedFieldReference() {
        if (type != FIELD_REFERENCE) {
            throw new IllegalStateException();
        }
        return (AnnotatedFieldReference) this;
    }

    /**
     * Casts this instance to {@link AnnotatedClassUsage}
     * @return this instance cast to {@link AnnotatedClassUsage}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#CLASS_USAGE}
     */
    public AnnotatedClassUsage asAnnotatedClassUsage() {
        if (type != CLASS_USAGE) {
            throw new IllegalStateException();
        }
        return (AnnotatedClassUsage) this;
    }

    /**
     * Casts this instance to {@link AnnotationOnUserClassUsage}
     * @return this instance cast to {@link AnnotationOnUserClassUsage}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#ANNOTATED_USER_CLASS}
     */
    public AnnotationOnUserClassUsage asAnnotationOnUserClassUsage() {
        if (type != ANNOTATED_USER_CLASS) {
            throw new IllegalStateException();
        }
        return (AnnotationOnUserClassUsage) this;
    }

    /**
     * Casts this instance to {@link AnnotationOnUserFieldUsage}
     * @return this instance cast to {@link AnnotationOnUserFieldUsage}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#ANNOTATED_USER_FIELD}
     */
    public AnnotationOnUserFieldUsage asAnnotationOnUserFieldUsage() {
        if (type != ANNOTATED_USER_FIELD) {
            throw new IllegalStateException();
        }
        return (AnnotationOnUserFieldUsage) this;
    }

    /**
     * Casts this instance to {@link AnnotationOnUserMethodUsage}
     * @return this instance cast to {@link AnnotationOnUserMethodUsage}
     * @throws IllegalStateException if {@link #getType()} is not {@link AnnotationUsageType#ANNOTATED_USER_METHOD}
     */
    public AnnotationOnUserMethodUsage asAnnotationOnUserMethodUsage() {
        if (type != ANNOTATED_USER_METHOD) {
            throw new IllegalStateException();
        }
        return (AnnotationOnUserMethodUsage) this;
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

    /**
     * Method to calculate the hash. We cache this to avoid having to call {@link #hashCode()} each time.
     * @return the calculated hash
     */
    protected int calculateHash() {
        return Objects.hash(annotations, type);
    }

    /**
     * When reading the bytecode, the class names will be in JVM format (e.g. {@code java/lang/Class}). We keep that for fast
     * lookups during the indexing. When returning the data to the users we convert to the more familiar
     * 'dot format' (i.e. {@code java.lang.Class})
     * @return the converted AnnotationUsage instance
     */
    protected abstract AnnotationUsage convertToDotFormat();
}
