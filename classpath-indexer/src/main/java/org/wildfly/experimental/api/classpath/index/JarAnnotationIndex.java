package org.wildfly.experimental.api.classpath.index;

import java.util.HashSet;
import java.util.Set;

public class JarAnnotationIndex extends AnnotationIndex {
    private JarAnnotationIndex(ResultBuilder builder) {
        super(builder.annotationName,
                builder.annotatedInterfaces,
                builder.annotatedClasses,
                builder.annotatedAnnotations,
                builder.annotatedMethods,
                builder.annotatedConstructors,
                builder.annotatedFields);
    }

    static ResultBuilder builder(String annotationName) {
        return new ResultBuilder(annotationName);
    }

    static class ResultBuilder {
        private final String annotationName;

        private final Set<String> annotatedInterfaces = new HashSet<>();
        private final Set<String> annotatedClasses = new HashSet<>();
        private final Set<String> annotatedAnnotations = new HashSet<>();

        private final Set<AnnotatedMethod> annotatedMethods = new HashSet<>();
        private final Set<AnnotatedConstructor> annotatedConstructors = new HashSet<>();
        private final Set<AnnotatedField> annotatedFields = new HashSet<>();

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

        public ResultBuilder addAnnotatedConstructor(AnnotatedConstructor constructor) {
            annotatedConstructors.add(constructor);
            return this;
        }

        ResultBuilder addAnnotatedField(AnnotatedField field) {
            annotatedFields.add(field);
            return this;
        }

        JarAnnotationIndex build() {
            return new JarAnnotationIndex(this);
        }

    }

}
