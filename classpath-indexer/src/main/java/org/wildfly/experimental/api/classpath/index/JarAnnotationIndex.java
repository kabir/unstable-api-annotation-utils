package org.wildfly.experimental.api.classpath.index;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>Contains the index of locations where an annotation has been found for a jar on the classpath.</p>
 *
 * <p>The intent in this library is that this contains all occurrences where an annotation was found
 * for a given classpath entry.</p>
 */

public class JarAnnotationIndex extends AnnotationIndex {

    /**
     * Constructor
     * @param builder the result builder
     */
    private JarAnnotationIndex(ResultBuilder builder) {
        super(builder.annotationName,
                builder.annotatedInterfaces,
                builder.annotatedClasses,
                builder.annotatedAnnotations,
                builder.annotatedMethods,
                builder.annotatedConstructors,
                builder.annotatedFields);
    }

    /**
     * Creates a ResultBuilder
     * @param annotationName the name of the annotation to search for
     * @return the result builder
     */
    static ResultBuilder builder(String annotationName) {
        return new ResultBuilder(annotationName);
    }

    /**
     * Used to add occurrances of an annotation found for a jar, and to construct
     * the JarAnnotationIndex
     */
    static class ResultBuilder {
        private final String annotationName;

        private final Set<String> annotatedInterfaces = new HashSet<>();
        private final Set<String> annotatedClasses = new HashSet<>();
        private final Set<String> annotatedAnnotations = new HashSet<>();

        private final Set<AnnotatedMethod> annotatedMethods = new HashSet<>();
        private final Set<AnnotatedConstructor> annotatedConstructors = new HashSet<>();
        private final Set<AnnotatedField> annotatedFields = new HashSet<>();

        /**
         * Constructor
         * @param annotationName the annotation to search for
         */
        public ResultBuilder(String annotationName) {
            this.annotationName = annotationName;
        }

        /**
         * Add an occurrence of an interface annotated with the annotation we are searching for
         * @param name the name of the interface
         * @return this builder
         */
        ResultBuilder addAnnotatedInterface(String name) {
            annotatedInterfaces.add(name);
            return this;
        }

        /**
         * Add an occurrence of a class annotated with the annotation we are searching for
         * @param name the name of the class
         * @return this builder
         */
        ResultBuilder addAnnotatedClass(String name) {
            annotatedClasses.add(name);
            return this;
        }

        /**
         * Add an occurrence of an annotation annotated with the annotation we are searching for
         * @param name the name of the annotation
         * @return this builder
         */
        ResultBuilder addAnnotatedAnnotation(String name) {
            annotatedAnnotations.add(name);
            return this;
        }

        /**
         * Add an occurrence of a method annotated with the annotation we are searching for
         * @param method the method
         * @return this builder
         */
        ResultBuilder addAnnotatedMethod(AnnotatedMethod method) {
            annotatedMethods.add(method);
            return this;
        }

        /**
         * Add an occurrence of a constructor annotated with the annotation we are searching for
         * @param constructor the constructor
         * @return this builder
         */
        public ResultBuilder addAnnotatedConstructor(AnnotatedConstructor constructor) {
            annotatedConstructors.add(constructor);
            return this;
        }

        /**
         * Add an occurrence of a field annotated with the annotation we are searching for
         * @param field the field
         * @return this builder
         */
        ResultBuilder addAnnotatedField(AnnotatedField field) {
            annotatedFields.add(field);
            return this;
        }

        /**
         * Create the JarAnnotationIndex from this builder
         * @return the JarAnnotationIndex
         */
        JarAnnotationIndex build() {
            return new JarAnnotationIndex(this);
        }

    }

}
