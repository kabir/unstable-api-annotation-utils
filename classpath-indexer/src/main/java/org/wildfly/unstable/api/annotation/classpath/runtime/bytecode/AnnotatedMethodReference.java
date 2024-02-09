package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;

import org.wildfly.unstable.api.annotation.classpath.index.OverallIndex;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.METHOD_REFERENCE;

/**
 * Records usage in user bytecode of a method that has been annotated with one of the annotations
 * we recorded as unstable api in the {@link OverallIndex}
 */
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

    /**
     * Gets the name of the class containing the method
     * @return the name of the class
     */
    public String getMethodClass() {
        return methodClass;
    }

    /**
     * Gets the name of the method
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the descriptor
     * @return the method descriptor
     */
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
