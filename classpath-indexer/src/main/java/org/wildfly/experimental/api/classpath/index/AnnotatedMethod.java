package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

class AnnotatedMethod {
    private final String className;

    private final ClassType classType;
    private final String methodName;
    private final String signature;


    AnnotatedMethod(String className, ClassType classType, String methodName, String signature) {
        this.className = className;
        this.classType = classType;
        this.methodName = methodName;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedMethod)) return false;
        AnnotatedMethod that = (AnnotatedMethod) o;
        return Objects.equals(className, that.className) && classType == that.classType && Objects.equals(methodName, that.methodName) && Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, classType, methodName, signature);
    }

    void save(PrintWriter writer, String separator) {
        writer.println(className + separator + classType + separator + methodName + separator + signature);
    }

    static AnnotatedMethod parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 4) {
            throw new IllegalArgumentException(s);
        }

        return new AnnotatedMethod(arr[0], ClassType.valueOf(arr[1]), arr[2], arr[3]);
    }


    enum ClassType {
        CLASS,
        INTERFACE,
        ANNOTATION
    }
}
