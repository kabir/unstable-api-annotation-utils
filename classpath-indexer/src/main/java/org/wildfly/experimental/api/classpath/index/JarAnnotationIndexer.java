package org.wildfly.experimental.api.classpath.index;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.JarIndexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Result;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class JarAnnotationIndexer {
    private final File file;
    private final String experimentalAnnotation;

    private final Set<String> excludedClasses;

    public JarAnnotationIndexer(File file, String annotation, Set<String> excludedClasses) {
        if (file == null || annotation == null || excludedClasses == null) {
            throw new NullPointerException("Null parameter");
        }
        this.file = file;
        this.experimentalAnnotation = annotation;
        this.excludedClasses = excludedClasses;
    }

    public JarAnnotationIndex scanForAnnotation() throws IOException {
        Set<String> foundClasses = new HashSet<>();
        Indexer indexer = new Indexer();
        Result result = JarIndexer.createJarIndex(file, indexer, false, true, false);
        Index index = result.getIndex();

        Collection<AnnotationInstance> annotations = index.getAnnotations(experimentalAnnotation);
        JarAnnotationIndex.ResultBuilder resultBuilder = JarAnnotationIndex.builder(experimentalAnnotation);
        for (AnnotationInstance annotation : annotations) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo classInfo = annotation.target().asClass();
                String className = classInfo.name().toString();
                if (!excludedClasses.contains(className)) {
                    if (classInfo.isAnnotation()) {
                        resultBuilder.addAnnotatedAnnotation(className);
                    } else if (classInfo.isInterface()) {
                        resultBuilder.addAnnotatedInterface(className);
                    } else if (classInfo.isDeclaration()) {
                        resultBuilder.addAnnotatedClass(className);
                    }
                }
            } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD || annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                MethodInfo methodInfo = annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER ?
                        annotation.target().asMethodParameter().method() : annotation.target().asMethod();
                ClassInfo classInfo = methodInfo.declaringClass();
                String className = classInfo.name().toString();
                if (!excludedClasses.contains(className)) {
                    if (methodInfo.isConstructor()) {
                        AnnotatedConstructor annotatedConstructor = new AnnotatedConstructor(className, methodInfo.descriptor());
                        resultBuilder.addAnnotatedConstructor(annotatedConstructor);
                    } else {
                        AnnotatedMethod annotatedMethod = new AnnotatedMethod(
                                className,
                                methodInfo.name(),
                                methodInfo.descriptor());

                        resultBuilder.addAnnotatedMethod(annotatedMethod);
                    }
                }
            } else if(annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                FieldInfo fieldInfo = annotation.target().asField();
                ClassInfo classInfo = fieldInfo.declaringClass();
                String className = classInfo.name().toString();
                if (!excludedClasses.contains(className)) {
                    AnnotatedField annotatedField = new AnnotatedField(className, fieldInfo.name());
                    resultBuilder.addAnnotatedField(annotatedField);
                }
            }
        }
        return resultBuilder.build();
    }
}
