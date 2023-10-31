package org.wildfly.experimental.api.classpath.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class AnnotationIndex {

    private static final String START_MARKER = "==";
    private static final String END_MARKER = "=/";
    private static final String INTERFACES = "=INTERFACES";
    private static final String CLASSES = "=CLASSES";
    private static final String ANNOTATIONS = "=ANNOTATIONS";
    private static final String METHODS = "=METHODS";
    private static final String CONSTRUCTORS = "=CONSTRUCTORS";
    private static final String FIELDS = "=FIELDS";

    private static final String MULTI_VALUE_FIELD_SEPARATOR = "±";


    private final String annotationName;
    private final Set<String> annotatedInterfaces;
    private final Set<String> annotatedClasses;
    private final Set<String> annotatedAnnotations;

    private final Set<AnnotatedMethod> annotatedMethods;
    private final Set<AnnotatedConstructor> annotatedConstructors;
    private final Set<AnnotatedField> annotatedFields;

    protected AnnotationIndex(String annotationName,
                              Set<String> annotatedInterfaces,
                              Set<String> annotatedClasses,
                              Set<String> annotatedAnnotations,
                              Set<AnnotatedMethod> annotatedMethods,
                              Set<AnnotatedConstructor> annotatedConstructors,
                              Set<AnnotatedField> annotatedFields) {
        this.annotationName = annotationName;
        this.annotatedInterfaces = annotatedInterfaces;
        this.annotatedClasses = annotatedClasses;
        this.annotatedAnnotations = annotatedAnnotations;
        this.annotatedMethods = annotatedMethods;
        this.annotatedConstructors = annotatedConstructors;
        this.annotatedFields = annotatedFields;
    }

    AnnotationIndex(AnnotationIndex annotationIndex) {
        this(annotationIndex.annotationName,
                annotationIndex.annotatedInterfaces,
                annotationIndex.annotatedClasses,
                annotationIndex.annotatedAnnotations,
                annotationIndex.annotatedMethods,
                annotationIndex.annotatedConstructors,
                annotationIndex.annotatedFields);
    }

    void addIndexEntries(AnnotationIndex jarAnnotationIndex) {
        annotatedInterfaces.addAll(jarAnnotationIndex.getAnnotatedInterfaces());
        annotatedClasses.addAll(jarAnnotationIndex.getAnnotatedClasses());
        annotatedAnnotations.addAll(jarAnnotationIndex.getAnnotatedAnnotations());
        annotatedMethods.addAll(jarAnnotationIndex.getAnnotatedMethods());
        annotatedConstructors.addAll(jarAnnotationIndex.getAnnotatedConstructors());
        annotatedFields.addAll(jarAnnotationIndex.getAnnotatedFields());
    }

    String getAnnotationName() {
        return annotationName;
    }

    Set<String> getAnnotatedInterfaces() {
        return annotatedInterfaces;
    }

    Set<String> getAnnotatedClasses() {
        return annotatedClasses;
    }

    Set<String> getAnnotatedAnnotations() {
        return annotatedAnnotations;
    }

    Set<AnnotatedMethod> getAnnotatedMethods() {
        return annotatedMethods;
    }

    Set<AnnotatedConstructor> getAnnotatedConstructors() {
        return annotatedConstructors;
    }

    Set<AnnotatedField> getAnnotatedFields() {
        return annotatedFields;
    }


    void save(PrintWriter writer) {
        if (!annotatedInterfaces.isEmpty()
                || !annotatedClasses.isEmpty()
                || !annotatedAnnotations.isEmpty()
                || !annotatedMethods.isEmpty()
                || !annotatedConstructors.isEmpty()
                || !annotatedFields.isEmpty()) {
            writer.println(START_MARKER);
            writer.println(annotationName);
            if (!annotatedInterfaces.isEmpty()) {
                writeSimpleStringEntries(writer, INTERFACES, annotatedInterfaces);
            }
            if (!annotatedClasses.isEmpty()) {
                writeSimpleStringEntries(writer, CLASSES, annotatedClasses);
            }
            if (!annotatedAnnotations.isEmpty()) {
                writeSimpleStringEntries(writer, ANNOTATIONS, annotatedAnnotations);
            }
            if (!annotatedMethods.isEmpty()) {
                writeObjectEntries(writer, METHODS, annotatedMethods, m -> m.save(writer, MULTI_VALUE_FIELD_SEPARATOR));
            }
            if (!annotatedConstructors.isEmpty()) {
                writeObjectEntries(writer, CONSTRUCTORS, annotatedConstructors, c -> c.save(writer, MULTI_VALUE_FIELD_SEPARATOR));
            }
            if (!annotatedFields.isEmpty()) {
                writeObjectEntries(writer, FIELDS, annotatedFields, f -> f.save(writer, MULTI_VALUE_FIELD_SEPARATOR));
            }
            writer.println(END_MARKER);
        }
    }

    static Map<String, AnnotationIndex> loadAll(BufferedReader reader) throws IOException {
        Map<String, AnnotationIndex> map = new HashMap<>();
        String line = reader.readLine();
        while (line != null) {
            if (line.equals(START_MARKER)) {
                AnnotationIndex index = load(reader);
                map.put(index.getAnnotationName(), index);
            }
            line = reader.readLine();
        }
        return map;
    }

    private static AnnotationIndex load(BufferedReader reader) throws IOException {
        Set<String> interfaces = new HashSet<>();
        Set<String> classes = new HashSet<>();
        Set<String> annotations = new HashSet<>();
        Set<AnnotatedMethod> methods = new HashSet<>();
        Set<AnnotatedConstructor> constructors = new HashSet<>();
        Set<AnnotatedField> fields = new HashSet<>();
        try {
            String annotation = reader.readLine();
            String line = reader.readLine();
            while (line != null) {
                switch (line) {
                    case INTERFACES:
                        interfaces = readSimpleStringLines(reader);
                        break;
                    case CLASSES:
                        classes = readSimpleStringLines(reader);
                        break;
                    case ANNOTATIONS:
                        annotations = readSimpleStringLines(reader);
                        break;
                    case METHODS:
                        methods = readLines(reader, s -> AnnotatedMethod.parseReadLine(s, MULTI_VALUE_FIELD_SEPARATOR));
                        break;
                    case CONSTRUCTORS:
                        constructors = readLines(reader, s -> AnnotatedConstructor.parseReadLine(s, MULTI_VALUE_FIELD_SEPARATOR));
                        break;
                    case FIELDS:
                        fields = readLines(reader, s -> AnnotatedField.parseReadLine(s, MULTI_VALUE_FIELD_SEPARATOR));
                        break;
                    case END_MARKER:
                        return new AnnotationIndex(annotation,
                                interfaces,
                                classes,
                                annotations,
                                methods,
                                constructors,
                                fields);

                }
                line = reader.readLine();
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("Premature end of file");
        }
        throw new RuntimeException("Premature end of file");
    }

    private static Set<String> readSimpleStringLines(BufferedReader reader) throws IOException {
        Set<String> results = readLines(reader, s -> s);
        return results;
    }

    private static <R> Set<R> readLines(BufferedReader reader, Function<String, R> parser) throws IOException {
        Set<R> results = new HashSet<>();
        String line = reader.readLine();
        while (!line.isEmpty()) {
            results.add(parser.apply(line));
            line = reader.readLine();
        }
        return results;
    }

    private void writeSimpleStringEntries(PrintWriter writer, String marker, Set<String> set) {
        writeObjectEntries(writer, marker, set, s -> writer.println(s));
    }

    private <T> void writeObjectEntries(PrintWriter writer, String marker, Set<T> set, Consumer<T> consumer) {
        writer.println(marker);
        for (T value : set) {
            consumer.accept(value);
        }
        writer.println();
    }


    /**
     * For testing
     * @param o the other index
     * @return true if the instances are the same or have the same contents
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotationIndex)) return false;
        AnnotationIndex that = (AnnotationIndex) o;
        return Objects.equals(annotationName, that.annotationName) && Objects.equals(annotatedInterfaces, that.annotatedInterfaces) && Objects.equals(annotatedClasses, that.annotatedClasses) && Objects.equals(annotatedAnnotations, that.annotatedAnnotations) && Objects.equals(annotatedMethods, that.annotatedMethods) && Objects.equals(annotatedConstructors, that.annotatedConstructors) && Objects.equals(annotatedFields, that.annotatedFields);
    }

}
