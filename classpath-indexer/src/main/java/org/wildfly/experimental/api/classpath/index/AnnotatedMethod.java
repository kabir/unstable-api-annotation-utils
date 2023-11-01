package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

class AnnotatedMethod {
    private final String className;
    private final String methodName;
    private final String descriptor;


    AnnotatedMethod(String className, String methodName, String descriptor) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedMethod)) return false;
        AnnotatedMethod that = (AnnotatedMethod) o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, descriptor);
    }

    void save(PrintWriter writer, String separator) {
        writer.println(className + separator + methodName + separator + descriptor);
    }

    static AnnotatedMethod parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 3) {
            throw new IllegalArgumentException(s);
        }

        return new AnnotatedMethod(arr[0], arr[1], arr[2]);
    }

    String getClassName() {
        return className;
    }

    String getMethodName() {
        return methodName;
    }

    String getDescriptor() {
        return descriptor;
    }

}
