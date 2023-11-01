package org.wildfly.experimental.api.classpath.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class RuntimeIndex {

    //Classes, and their annotations
    private final Map<String, Set<String>> classesWithAnnotations;

    // Keys in 'nested order' are className, methodname, method signature. The set is the annotations for the method.
    private final Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations;

    // Keys in 'nested order' are className, fieldName. The set is the annotations for the method.
    private final Map<String, Map<String, Set<String>>> fieldsWithAnnotations;



    private RuntimeIndex(Map<String, Set<String>> classesWithAnnotations,
                         Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations,
                         Map<String, Map<String, Set<String>>> fieldsWithAnnotations) {
        this.classesWithAnnotations = classesWithAnnotations;
        this.methodsWithAnnotations = methodsWithAnnotations;
        this.fieldsWithAnnotations = fieldsWithAnnotations;
    }

    public static RuntimeIndex load(Path indexFile, Path... additional) throws IOException {
        OverallIndex overallIndex = OverallIndex.load(indexFile, additional);
        Map<String, Set<String>> classesWithAnnotations = new HashMap<>();
        Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations = new HashMap<>();
        Map<String, Map<String, Set<String>>> fieldsWithAnnotations = new HashMap<>();

        for (String annotation : overallIndex.getAnnotations()) {
            AnnotationIndex annotationIndex = overallIndex.getAnnotationIndex(annotation);
            addClassesWithAnnotations(annotation, annotationIndex, classesWithAnnotations);
            addMethodsWithAnnotations(annotation, annotationIndex, methodsWithAnnotations);
            addFieldsWithAnnotations(annotation, annotationIndex, fieldsWithAnnotations);
        }

        return new RuntimeIndex(classesWithAnnotations, methodsWithAnnotations, fieldsWithAnnotations);
    }

    private static void addClassesWithAnnotations(
            String annotation,
            AnnotationIndex annotationIndex,
            Map<String, Set<String>> classesWithAnnotations) {

        // TODO merge these two into simply annotatedClasses?
        for (String clazz : annotationIndex.getAnnotatedClasses()) {
            String vmClass = convertClassNameToVmFormat(clazz);
            Set<String> annotations = getOrCreate(classesWithAnnotations, vmClass, () -> new HashSet<>());
            annotations.add(annotation);
        }
        for (String clazz : annotationIndex.getAnnotatedInterfaces()) {
            String vmClass = convertClassNameToVmFormat(clazz);
            Set<String> annotations = getOrCreate(classesWithAnnotations, vmClass, () -> new HashSet<>());
            annotations.add(annotation);
        }
        // TODO I don't think we need to handle AnnotationIndex.getAnnotatedAnnotations() here since we will use jandex
        // to pick those out
    }

    private static void addMethodsWithAnnotations(String annotation, AnnotationIndex annotationIndex, Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations) {
        for (AnnotatedMethod annotatedMethod : annotationIndex.getAnnotatedMethods()) {
            String vmClass = convertClassNameToVmFormat(annotatedMethod.getClassName());
            String methodname = annotatedMethod.getMethodName();
            String signature = annotatedMethod.getSignature();

            Map<String, Map<String, Set<String>>> methodsForClass = getOrCreate(methodsWithAnnotations, vmClass, () -> new HashMap<>());
            Map<String, Set<String>> signaturesForMethod = getOrCreate(methodsForClass, methodname, () -> new HashMap<>());
            Set<String> annotationsForMethod = getOrCreate(signaturesForMethod, signature, () -> new HashSet<>());
            annotationsForMethod.add(annotation);
        }
    }

    private static void addFieldsWithAnnotations(String annotation, AnnotationIndex annotationIndex, Map<String, Map<String, Set<String>>> fieldsWithAnnotations) {
        for (AnnotatedField annotatedField : annotationIndex.getAnnotatedFields()) {
            String vmClass = convertClassNameToVmFormat(annotatedField.getClassName());
            String fieldName = annotatedField.getFieldName();

            Map<String, Set<String>> fieldsForClass = getOrCreate(fieldsWithAnnotations, vmClass, () -> new HashMap<>());
            Set<String> annotationsForMethod = getOrCreate(fieldsForClass, fieldName, () -> new HashSet<>());
            annotationsForMethod.add(annotation);
        }
    }


    private static <T> T getOrCreate(Map<String, T> map, String key, Supplier<T> factory) {
        T set = map.get(key);
        if (set == null) {
            set = factory.get();
            map.put(key, set);
        }
        return set;
    }

    private static String convertClassNameToVmFormat(String s) {
        return s.replaceAll("\\.", "/");
    }

    public Set<String> geClassAnnotations(String superClass) {
        return classesWithAnnotations.get(superClass);
    }
}
