package org.wildfly.experimental.api.classpath.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RuntimeIndex {

    // Not used for matching, but a hint that we should be picking out information for a class
    private final Set<String> allClasses;
    private final Map<String, Set<String>> classesWithAnnotations;


    private RuntimeIndex(Set<String> allClasses, Map<String, Set<String>> classesWithAnnotations) {
        this.allClasses = allClasses;
        this.classesWithAnnotations = classesWithAnnotations;
    }

    public static RuntimeIndex load(Path indexFile, Path... additional) throws IOException {
        OverallIndex overallIndex = OverallIndex.load(indexFile, additional);
        Set<String> allClasses = new HashSet<>();
        Map<String, Set<String>> classesWithAnnotations = new HashMap<>();

        for (String annotation : overallIndex.getAnnotations()) {
            AnnotationIndex annotationIndex = overallIndex.getAnnotationIndex(annotation);
            addClassesWithAnnotations(annotation, annotationIndex, allClasses, classesWithAnnotations);
        }


        return new RuntimeIndex(allClasses, classesWithAnnotations);
    }

    private static void addClassesWithAnnotations(
            String annotation,
            AnnotationIndex annotationIndex,
            Set<String> allClasses,
            Map<String, Set<String>> classesWithAnnotations) {

        // TODO merge these two into simply annotatedClasses?
        for (String clazz : annotationIndex.getAnnotatedClasses()) {
            String vmClass = convertClassNameToVmFormat(clazz);
            allClasses.add(vmClass);
            Set<String> annotations = getOrCreate(classesWithAnnotations, vmClass);
            annotations.add(annotation);
        }
        for (String clazz : annotationIndex.getAnnotatedInterfaces()) {
            String vmClass = convertClassNameToVmFormat(clazz);
            allClasses.add(vmClass);
            Set<String> annotations = getOrCreate(classesWithAnnotations, vmClass);
            annotations.add(annotation);
        }
        // TODO I don't think we need to handle AnnotationIndex.getAnnotatedAnnotations() here since we will use jandex
        // to pick those out
    }

    private static Set<String> getOrCreate(Map<String, Set<String>> map, String key) {
        Set<String> set = map.get(key);
        if (set == null) {
            set = new HashSet<>();
            map.put(key, set);
        }
        return set;
    }

    private static String convertClassNameToVmFormat(String s) {
        return s.replaceAll("\\.", "/");
    }


}
