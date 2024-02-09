package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;

import org.wildfly.unstable.api.annotation.classpath.index.OverallIndex;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_FIELD;

/**
 * Records a field in user bytecode that has been annotated with one of the annotations
 * we recorded as being unstable in the {@link OverallIndex}
 */
public class AnnotationOnUserFieldUsage extends AnnotationUsage {
    private final String clazz;
    private final String fieldName;

    AnnotationOnUserFieldUsage(String clazz, String fieldName, Set<String> annotations) {
        super(annotations, ANNOTATED_USER_FIELD);
        this.clazz = clazz;
        this.fieldName = fieldName;
    }

    @Override
    protected AnnotationOnUserFieldUsage convertToDotFormat() {
        return this;
    }

    /**
     * Gets the name of the class containing the field
     * @return the class name
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Gets the name of the field
     * @return the field
     */
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnnotationOnUserFieldUsage that = (AnnotationOnUserFieldUsage) o;
        return Objects.equals(clazz, that.clazz) && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), clazz, fieldName);
    }
}
