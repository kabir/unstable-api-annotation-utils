package org.wildfly.experimental.api.classpath.indexer;

import java.util.HashSet;
import java.util.Set;

public class JarAnnotationIndexerResult {
    private final String annotationName;
    private final Set<String> annotatedInterfaces;
    private final Set<String> annotatedClasses;
    private final Set<String> annotatedAnnotations;

    private JarAnnotationIndexerResult(ResultBuilder builder) {
        this.annotationName = builder.annotationName;
        this.annotatedInterfaces = builder.annotatedInterfaces;
        this.annotatedClasses = builder.annotatedClasses;
        this.annotatedAnnotations = builder.annotatedAnnotations;
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

    static ResultBuilder builder(String annotationName) {
        return new ResultBuilder(annotationName);
    }

    static class ResultBuilder {
        private final String annotationName;

        private Set<String> annotatedInterfaces = new HashSet<>();
        private Set<String> annotatedClasses = new HashSet<>();
        private Set<String> annotatedAnnotations = new HashSet<>();

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

        JarAnnotationIndexerResult build() {
            return new JarAnnotationIndexerResult(this);
        }
    }

}
