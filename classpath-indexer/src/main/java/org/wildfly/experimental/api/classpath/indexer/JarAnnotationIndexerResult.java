package org.wildfly.experimental.api.classpath.indexer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class JarAnnotationIndexerResult {
    private final String annotationName;
    private final Set<String> annotatedInterfaces;
    private final Set<String> annotatedClasses;
    private final Set<String> annotatedAnnotations;

    private final Set<AnnotatedMethod> annotatedMethods;

    private JarAnnotationIndexerResult(ResultBuilder builder) {
        this.annotationName = builder.annotationName;
        this.annotatedInterfaces = builder.annotatedInterfaces;
        this.annotatedClasses = builder.annotatedClasses;
        this.annotatedAnnotations = builder.annotatedAnnotations;
        annotatedMethods = builder.annotatedMethods;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public Set<String> getAnnotatedInterfaces() {
        return annotatedInterfaces;
    }

    public Set<String> getAnnotatedClasses() {
        return annotatedClasses;
    }

    public Set<String> getAnnotatedAnnotations() {
        return annotatedAnnotations;
    }

    public Set<AnnotatedMethod> getAnnotatedMethods() {
        return annotatedMethods;
    }

    static ResultBuilder builder(String annotationName) {
        return new ResultBuilder(annotationName);
    }

    static class ResultBuilder {
        private final String annotationName;

        private Set<String> annotatedInterfaces = new HashSet<>();
        private Set<String> annotatedClasses = new HashSet<>();
        private Set<String> annotatedAnnotations = new HashSet<>();

        private Set<AnnotatedMethod> annotatedMethods = new HashSet<>();

        public ResultBuilder(String annotationName) {
            this.annotationName = annotationName;
        }

        ResultBuilder addAnnotatedInterface(String name) {
            annotatedInterfaces.add(name);
            return this;
        }

        ResultBuilder addAnnotatedClass(String name) {
            annotatedClasses.add(name);
            return this;
        }

        ResultBuilder addAnnotatedAnnotation(String name) {
            annotatedAnnotations.add(name);
            return this;
        }

        ResultBuilder addAnnotatedMethod(AnnotatedMethod method) {
            annotatedMethods.add(method);
            return this;
        }

        JarAnnotationIndexerResult build() {
            return new JarAnnotationIndexerResult(this);
        }
    }

    static class AnnotatedMethod {
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

        public String getClassName() {
            return className;
        }

        public ClassType getClassType() {
            return classType;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getSignature() {
            return signature;
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
    }

    public enum ClassType {
        CLASS,
        INTERFACE,
        ANNOTATION
    }
}
