package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.ANNOTATED_USER_METHOD;

public class AnnotationOnUserMethodUsage extends AnnotationUsage {
    private final String clazz;
    private final String methodName;
    private final String descriptor;

    AnnotationOnUserMethodUsage(String clazz, String methodName, String descriptor, Set<String> annotations) {
        super(annotations, ANNOTATED_USER_METHOD);
        this.clazz = clazz;
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    @Override
    protected AnnotationOnUserMethodUsage convertToDotFormat() {
        return this;
    }

    public String getClazz() {
        return clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnnotationOnUserMethodUsage that = (AnnotationOnUserMethodUsage) o;
        return Objects.equals(clazz, that.clazz) && Objects.equals(methodName, that.methodName) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), clazz, methodName, descriptor);
    }
}
