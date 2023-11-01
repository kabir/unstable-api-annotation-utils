package org.wildfly.experimental.api.classpath.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class RuntimeIndex {

    // Classes, interfaces and annotations, and their annotations
    // We are including annotations here since users might decide to implement an annotation interface
    // that is marked as experimental
    private final Map<String, Set<String>> allClassesWithAnnotations;

    // Annotations with annotations. Although these are also part of allClassesWithAnnotations,
    // this field will be needed as input to the Jandex scanning for annotation usage
    private final Map<String, Set<String>> annotationsWithAnnotations;

    // Keys in 'nested order' are className, methodname, method signature. The set is the annotations for the method.
    private final Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations;

    // Keys in 'nested order' are className, fieldName. The set is the annotations for the method.
    private final Map<String, Map<String, Set<String>>> fieldsWithAnnotations;



    private RuntimeIndex(Map<String, Set<String>> allClassesWithAnnotations,
                         Map<String, Set<String>> annotationsWithAnnotations,
                         Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations,
                         Map<String, Map<String, Set<String>>> fieldsWithAnnotations) {
        this.allClassesWithAnnotations = allClassesWithAnnotations;
        this.annotationsWithAnnotations = annotationsWithAnnotations;
        this.methodsWithAnnotations = methodsWithAnnotations;
        this.fieldsWithAnnotations = fieldsWithAnnotations;
    }

    public static RuntimeIndex load(Path indexFile, Path... additional) throws IOException {
        OverallIndex overallIndex = OverallIndex.load(indexFile, additional);
        Map<String, Set<String>> allClassesWithAnnotations = new HashMap<>();
        Map<String, Set<String>> annotationsWithAnnotations = new HashMap<>();
        Map<String, Map<String, Map<String, Set<String>>>> methodsWithAnnotations = new HashMap<>();
        Map<String, Map<String, Set<String>>> fieldsWithAnnotations = new HashMap<>();

        for (String annotation : overallIndex.getAnnotations()) {
            AnnotationIndex annotationIndex = overallIndex.getAnnotationIndex(annotation);
            addClassesWithAnnotations(annotation, annotationIndex, allClassesWithAnnotations, annotationsWithAnnotations);
            addMethodsWithAnnotations(annotation, annotationIndex, methodsWithAnnotations);
            addFieldsWithAnnotations(annotation, annotationIndex, fieldsWithAnnotations);
        }

        return new RuntimeIndex(allClassesWithAnnotations, annotationsWithAnnotations, methodsWithAnnotations, fieldsWithAnnotations);
    }

    private static void addClassesWithAnnotations(
            String annotation,
            AnnotationIndex annotationIndex,
            Map<String, Set<String>> classesWithAnnotations,
            Map<String, Set<String>> annotationsWithAnnotations) {

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
        for (String clazz : annotationIndex.getAnnotatedAnnotations()) {
            String vmClass = convertClassNameToVmFormat(clazz);
            Set<String> classAnnotations = getOrCreate(classesWithAnnotations, vmClass, () -> new HashSet<>());
            classAnnotations.add(annotation);
            Set<String> annAnnotations = getOrCreate(annotationsWithAnnotations, vmClass, () -> new HashSet<>());
            annAnnotations.add(annotation);
        }
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

    public Set<String> getAllClassesWithAnnotations(String clazz) {
        return allClassesWithAnnotations.get(clazz);
    }

    public Set<String> getAnnotationsWithAnnotations(String clazz) {
        return annotationsWithAnnotations.get(clazz);
    }
}
