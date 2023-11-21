package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

public class AnnotatedMethodReference extends AnnotationWithSourceClassUsage {
    private final String methodClass;
    private final String methodName;
    private final String descriptor;

    AnnotatedMethodReference(Set<String> annotations, String className, String methodClass, String methodName, String descriptor) {
        super(annotations, METHOD_REFERENCE, className);
        this.methodClass = methodClass;
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    public String getMethodClass() {
        return methodClass;
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
        AnnotatedMethodReference that = (AnnotatedMethodReference) o;
        return Objects.equals(methodClass, that.methodClass) && Objects.equals(methodName, that.methodName) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    // Don't override hashcode here, the root class will do some caching
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), methodClass, methodName, descriptor);
    }

    @Override
    protected AnnotationUsage convertToDotFormat() {
        return new AnnotatedMethodReference(
                annotations,
                convertClassNameToDotFormat(sourceClass),
                convertClassNameToDotFormat(methodClass),
                methodName,
                descriptor);
    }
}
