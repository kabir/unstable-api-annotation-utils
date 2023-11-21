package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.FIELD_REFERENCE;

public class AnnotatedFieldReference extends AnnotationWithSourceClassUsage {
    private final String fieldClass;
    private final String fieldName;

    AnnotatedFieldReference(Set<String> annotations, String className, String fieldClass, String fieldName) {
        super(annotations, FIELD_REFERENCE, className);
        this.fieldClass = fieldClass;
        this.fieldName = fieldName;
    }

    public String getFieldClass() {
        return fieldClass;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnnotatedFieldReference that = (AnnotatedFieldReference) o;
        return Objects.equals(fieldClass, that.fieldClass) && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    // Don't override hashcode here, the root class will do some caching
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), fieldClass, fieldName);
    }

    @Override
    protected AnnotationUsage convertToDotFormat() {
        return new AnnotatedFieldReference(
                annotations,
                convertClassNameToDotFormat(sourceClass),
                convertClassNameToDotFormat(fieldClass), fieldName);
    }
}
