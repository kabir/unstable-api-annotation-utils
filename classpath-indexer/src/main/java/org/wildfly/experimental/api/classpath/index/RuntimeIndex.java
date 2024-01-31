package org.wildfly.experimental.api.classpath.index;

import org.wildfly.experimental.api.classpath.runtime.bytecode.ReusableStreams;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * In a nutshell this class reads an {@link OverallIndex}, and stores it in an optimised way so that it can
 * be read quickly. This is used by the {@link org.wildfly.experimental.api.classpath.runtime.bytecode.ClassInfoScanner}
 * to avoid needing to convert all the Utf8Info entries in the bytecode to strings.
 */
public class RuntimeIndex {
    public static final String BYTECODE_CONSTRUCTOR_NAME = "<init>";

    private static final byte[] OBJECT_BYTES = new byte[] {
            // Length = 16
            0, 16,
            // j     a     v     o     /
            0x6a, 0x61, 0x76, 0x61, 0x2f,
            // l     a     n     g     /
            0x6c, 0x61, 0x6e, 0x67, 0x2f,
            // O     b     j     e     c     t
            0x4f, 0x62, 0x6a, 0x65, 0x63, 0x74
    };


    /**
     * ByteArrayKey for java/lang/Object
     */
    public static final ByteArrayKey JAVA_LANG_OBJECT_KEY = ByteArrayKey.create(OBJECT_BYTES, 0, OBJECT_BYTES.length);

    private static final ByteArrayOutputStream BYTE_ARRAY_OUTPUT_STREAM = new ByteArrayOutputStream(2048);


    /**
     * {@code }<init>} (i.e. the bytecode name of a constructor) as a ByteArrayKey
     */
    public static final ByteArrayKey BYTECODE_CONSTRUCTOR_KEY;
    static {
        try {
            BYTECODE_CONSTRUCTOR_KEY = convertStringToByteArrayKey(BYTECODE_CONSTRUCTOR_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extended classed, implements interfaces and annotations, and the annotations they have been annotated with.
     * We are including annotations here since users might decide to implement an annotation interface
     * that is marked as experimental
     */
    private final Map<ByteArrayKey, Set<String>> allClassesWithAnnotations;

    //

    /**
     * Annotations with annotations. Although these are also part of {@link #allClassesWithAnnotations}, this field
     * will be needed as input to the Jandex scanning for annotation usage
     */
    private final Map<String, Set<String>> annotationsWithAnnotations;


    /**
     * Keys in 'nested order' are class name, method name, descriptor. The set is the annotations for the method
     * pointed at pu each entry.
     */
    private final Map<ByteArrayKey, Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>>> methodsWithAnnotations;

    /**
     * Keys in 'nested order' are class name, field name. The set is the annotations for the field.
     */
    private final Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> fieldsWithAnnotations;


    /**
     * The names of all classes found indexed by their ByteArrayKey
     */
    private final Map<ByteArrayKey, String> classNamesByKey;

    /**
     * The ByteArrayKeys of all classes found indexed by their string name
     */
    private final Map<String, ByteArrayKey> classKeysByName;

    /**
     * The names of all method names found indexed by their ByteArrayKey
     */
    private final Map<ByteArrayKey, String> methodNamesByKey;

    /**
     * The names of all field names found indexed by their ByteArrayKey
     */
    private final Map<ByteArrayKey, String> fieldNamesByKey;

    /**
     * The string representations of all method descriptors found indexed by their ByteArrayKey
     */
    private final Map<ByteArrayKey, String> methodDescriptorsByKey;


    private RuntimeIndex(Map<ByteArrayKey, Set<String>> allClassesWithAnnotations,
                         Map<String, Set<String>> annotationsWithAnnotations,
                         Map<ByteArrayKey, Map<ByteArrayKey,
                             Map<ByteArrayKey, Set<String>>>> methodsWithAnnotations,
                         Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> fieldsWithAnnotations,
                         Map<ByteArrayKey, String> classNamesByKey,
                         Map<String, ByteArrayKey> classKeysByName,
                         Map<ByteArrayKey, String> methodNamesByKey,
                         Map<ByteArrayKey, String> fieldNamesByKey,
                         Map<ByteArrayKey, String> methodDescriptorsByKey) {
        this.allClassesWithAnnotations = Collections.unmodifiableMap(allClassesWithAnnotations);
        this.annotationsWithAnnotations = Collections.unmodifiableMap(annotationsWithAnnotations);
        this.methodsWithAnnotations = Collections.unmodifiableMap(methodsWithAnnotations);
        this.fieldsWithAnnotations = Collections.unmodifiableMap(fieldsWithAnnotations);
        this.classNamesByKey = Collections.unmodifiableMap(classNamesByKey);
        this.classKeysByName = Collections.unmodifiableMap(classKeysByName);
        this.methodNamesByKey = Collections.unmodifiableMap(methodNamesByKey);
        this.fieldNamesByKey = Collections.unmodifiableMap(fieldNamesByKey);
        this.methodDescriptorsByKey = Collections.unmodifiableMap(methodDescriptorsByKey);
    }

    /**
     * Loads the runtime index from a file containing a serialized index, and creates a RuntimeIndex instance with the information.
     * @param indexFile the location of the index file
     * @param additional additional index file locations
     * @return the created runtime index
     * @throws IOException if there are problems reading any of the files
     */
    public static RuntimeIndex load(Path indexFile, Path... additional) throws IOException {
        OverallIndex overallIndex = OverallIndex.load(indexFile, additional);
        return convertOverallIndexToRuntimeIndex(overallIndex);
    }

    /**
     * Loads the runtime index from locations specified as URLs, and creates a RuntimeIndex instance with the information.
     * The URLs should point to locations containing a serialized index.
     * @param urls the urls containing serialized indexes
     * @return the created overall index
     * @throws IOException if there are problems reading any of the URLs
     */
    public static RuntimeIndex load(List<URL> urls) throws IOException {
        OverallIndex overallIndex = OverallIndex.load(urls);
        return convertOverallIndexToRuntimeIndex(overallIndex);
    }

    private static RuntimeIndex convertOverallIndexToRuntimeIndex(OverallIndex overallIndex) {
        Map<ByteArrayKey, String> classNamesByKey = new HashMap<>();
        Map<String, ByteArrayKey> classKeysByName = new HashMap<>();
        Map<ByteArrayKey, String> methodNamesByKey = new HashMap<>();
        Map<ByteArrayKey, String> fieldNamesByKey = new HashMap<>();
        Map<ByteArrayKey, String> methodDescriptorsByKey = new HashMap<>();
        Map<ByteArrayKey, Set<String>> allClassesWithAnnotations = new HashMap<>();
        Map<String, Set<String>> annotationsWithAnnotations = new HashMap<>();
        Map<ByteArrayKey, Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>>> methodsWithAnnotations = new HashMap<>();
        Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> fieldsWithAnnotations = new HashMap<>();

        for (String annotation : overallIndex.getAnnotations()) {
            AnnotationIndex annotationIndex = overallIndex.getAnnotationIndex(annotation);
            addClassesWithAnnotations(annotation, annotationIndex, allClassesWithAnnotations, annotationsWithAnnotations, classNamesByKey, classKeysByName);
            addMethodsWithAnnotations(annotation, annotationIndex, methodsWithAnnotations, methodNamesByKey, methodDescriptorsByKey, classNamesByKey, classKeysByName);
            // On byte code level the only difference between a constructor and method is the name of the constructor
            // so we add the constructor to the methodsWithAnnotations set
            addConstructorsWithAnnotations(annotation, annotationIndex, methodsWithAnnotations, methodNamesByKey, methodDescriptorsByKey, classNamesByKey, classKeysByName);
            addFieldsWithAnnotations(annotation, annotationIndex, fieldsWithAnnotations, fieldNamesByKey, classNamesByKey, classKeysByName);
        }

        return new RuntimeIndex(allClassesWithAnnotations, annotationsWithAnnotations, methodsWithAnnotations, fieldsWithAnnotations, classNamesByKey, classKeysByName, methodNamesByKey, fieldNamesByKey, methodDescriptorsByKey);
    }

    private static void addClassesWithAnnotations(
            String annotation,
            AnnotationIndex annotationIndex,
            Map<ByteArrayKey, Set<String>> classesWithAnnotations,
            Map<String, Set<String>> annotationsWithAnnotations,
            Map<ByteArrayKey, String> classNamesByKey,
            Map<String, ByteArrayKey> classKeysByName) {

        for (String clazz : annotationIndex.getAnnotatedClasses()) {
            ByteArrayKey vmClass = convertStringToByteArrayKey(convertClassNameToVmFormat(clazz));
            Set<String> annotations = classesWithAnnotations.computeIfAbsent(vmClass, k -> new HashSet<>());
            annotations.add(annotation);
            classNamesByKey.put(vmClass, clazz);
            classKeysByName.put(clazz, vmClass);
        }
        for (String clazz : annotationIndex.getAnnotatedInterfaces()) {
            ByteArrayKey vmClass = convertStringToByteArrayKey(convertClassNameToVmFormat(clazz));
            Set<String> annotations = classesWithAnnotations.computeIfAbsent(vmClass, k -> new HashSet<>());
            annotations.add(annotation);
            classNamesByKey.put(vmClass, clazz);
            classKeysByName.put(clazz, vmClass);
        }
        for (String clazz : annotationIndex.getAnnotatedAnnotations()) {
            ByteArrayKey vmClass = convertStringToByteArrayKey(convertClassNameToVmFormat(clazz));
            Set<String> classAnnotations = classesWithAnnotations.computeIfAbsent(vmClass, k -> new HashSet<>());
            classAnnotations.add(annotation);
            // Since we use Jandex rather than bytecode inspection for this in the RuntimeIndex, just use the raw class name here
            Set<String> annAnnotations = annotationsWithAnnotations.computeIfAbsent(clazz, k -> new HashSet<>());
            annAnnotations.add(annotation);
        }
    }

    private static void addMethodsWithAnnotations(String annotation, AnnotationIndex annotationIndex, Map<ByteArrayKey, Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>>> methodsWithAnnotations, Map<ByteArrayKey, String> methodNamesByKey, Map<ByteArrayKey, String> methodDescriptorsByKey, Map<ByteArrayKey, String> classNamesByKey, Map<String, ByteArrayKey> classKeysByName) {
        for (AnnotatedMethod annotatedMethod : annotationIndex.getAnnotatedMethods()) {
            ByteArrayKey vmClass = convertStringToByteArrayKey(convertClassNameToVmFormat(annotatedMethod.getClassName()));
            ByteArrayKey methodname = convertStringToByteArrayKey(annotatedMethod.getMethodName());
            ByteArrayKey descriptor = convertStringToByteArrayKey(annotatedMethod.getDescriptor());

            classNamesByKey.put(vmClass, annotatedMethod.getClassName());
            classKeysByName.put(annotatedMethod.getClassName(), vmClass);
            methodNamesByKey.put(methodname, annotatedMethod.getMethodName());
            methodDescriptorsByKey.put(descriptor, annotatedMethod.getDescriptor());

            Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> methodsForClass = methodsWithAnnotations.computeIfAbsent(vmClass, k -> new HashMap<>());
            Map<ByteArrayKey, Set<String>> descriptorsForMethod = methodsForClass.computeIfAbsent(methodname, k -> new HashMap<>());
            Set<String> annotationsForMethod = descriptorsForMethod.computeIfAbsent(descriptor, k -> new HashSet<>());
            annotationsForMethod.add(annotation);
        }
    }

    private static void addConstructorsWithAnnotations(String annotation, AnnotationIndex annotationIndex, Map<ByteArrayKey, Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>>> methodsWithAnnotations, Map<ByteArrayKey, String> methodNamesByKey, Map<ByteArrayKey, String> methodDescriptorsByKey, Map<ByteArrayKey, String> classNamesByKey, Map<String, ByteArrayKey> classKeysByName) {
        methodNamesByKey.put(BYTECODE_CONSTRUCTOR_KEY, BYTECODE_CONSTRUCTOR_NAME);

        // On byte code level the only difference between a constructor and method is the name of the constructor
        // so we add the constructor to the methodsWithAnnotations set
        for (AnnotatedConstructor annotatedConstructor : annotationIndex.getAnnotatedConstructors()) {
            ByteArrayKey vmClass = convertStringToByteArrayKey(convertClassNameToVmFormat(annotatedConstructor.getClassName()));
            ByteArrayKey descriptor = convertStringToByteArrayKey(annotatedConstructor.getDescriptor());

            classNamesByKey.put(vmClass, annotatedConstructor.getClassName());
            classKeysByName.put(annotatedConstructor.getClassName(), vmClass);
            methodDescriptorsByKey.put(descriptor, annotatedConstructor.getDescriptor());

            Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> methodsForClass = methodsWithAnnotations.computeIfAbsent(vmClass, k -> new HashMap<>());
            Map<ByteArrayKey, Set<String>> descriptorsForMethod = methodsForClass.computeIfAbsent(BYTECODE_CONSTRUCTOR_KEY, k -> new HashMap<>());
            Set<String> annotationsForMethod = descriptorsForMethod.computeIfAbsent(descriptor, k -> new HashSet<>());
            annotationsForMethod.add(annotation);
        }
    }

    private static void addFieldsWithAnnotations(String annotation, AnnotationIndex annotationIndex, Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> fieldsWithAnnotations, Map<ByteArrayKey, String> fieldNamesByKey, Map<ByteArrayKey, String> classNamesByKey, Map<String, ByteArrayKey> classKeysByName) {
        for (AnnotatedField annotatedField : annotationIndex.getAnnotatedFields()) {
            ByteArrayKey vmClass = convertStringToByteArrayKey(convertClassNameToVmFormat(annotatedField.getClassName()));
            ByteArrayKey fieldName = convertStringToByteArrayKey(annotatedField.getFieldName());

            classNamesByKey.put(vmClass, annotatedField.getClassName());
            classKeysByName.put(annotatedField.getClassName(), vmClass);
            fieldNamesByKey.put(fieldName, annotatedField.getFieldName());

            Map<ByteArrayKey, Set<String>> fieldsForClass = fieldsWithAnnotations.computeIfAbsent(vmClass, k -> new HashMap<>());
            Set<String> annotationsForMethod = fieldsForClass.computeIfAbsent(fieldName, k -> new HashSet<>());
            annotationsForMethod.add(annotation);
        }
    }

    private static ByteArrayKey convertStringToByteArrayKey(String s) {
        BYTE_ARRAY_OUTPUT_STREAM.reset();
        try (DataOutputStream dout = new DataOutputStream(BYTE_ARRAY_OUTPUT_STREAM)) {
            dout.writeUTF(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ByteArrayKey(BYTE_ARRAY_OUTPUT_STREAM.toByteArray());
    }

    /** Converts a class name in dotname format (e.g. {@code org.acme.MyClass}) to JVM format
     * (e.g. {@code org/acme/MyClass})
     *
     * @param s the class name
     * @return the converted class name
     */
    public static String convertClassNameToVmFormat(String s) {
        return s.replaceAll("\\.", "/");
    }

    /** Converts a class name in JVM format (e.g. {@code org/acme/MyClass}) to dotname format
     * (e.g. {@code org.acme.MyClass})
     *
     * @param s the class name
     * @return the converted class name
     */
    public static String convertClassNameToDotFormat(String s) {
        return s.replaceAll("/", ".");
    }

    /**
     * Gets the annotations for a class
     *
     * @param key the name of the class
     * @return the annotation names. May be {@code null} if there are none
     */
    public Set<String> getAnnotationsForClass(ByteArrayKey key) {
        return allClassesWithAnnotations.get(key);
    }

    /**
     * Gets the annotations for an annotation
     * @param annotation the name of the annotation
     * @return the annotation names. May be {@code null} if there are none
     */
    public Set<String> getAnnotationsForAnnotation(String annotation) {
        return annotationsWithAnnotations.get(annotation);
    }

    /**
     * Gets all the annotations which have been annotated with one of annotations we searched for when
     * creating the {@link OverallIndex}
     * @return the annotation names.
     */
    public Set<String> getAnnotatedAnnotations() {
        return annotationsWithAnnotations.keySet();
    }

    /**
     * Get the annotations for a method from the information in the {@link OverallIndex}
     *
     * @param methodClass the name of the class containing the method
     * @param methodName the name of the method
     * @param methodDescriptor the method descriptor
     * @return the annotation names. May be {@code null} if there are none
     */
    public Set<String> getAnnotationsForMethod(ByteArrayKey methodClass, Supplier<ByteArrayKey> methodName, Supplier<ByteArrayKey> methodDescriptor) {
        Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> methodsInClass = methodsWithAnnotations.get(methodClass);
        if (methodsInClass == null) {
            return null;
        }
        Map<ByteArrayKey, Set<String>> methodDescriptors = methodsInClass.get(methodName.get());
        if (methodDescriptors == null) {
            return null;
        }
        return methodDescriptors.get(methodDescriptor.get());
    }

    /**
     * Get the annotations for a field from the information in the {@link OverallIndex}
     * @param fieldClass the name of the class containing the field
     * @param fieldName the name of the field
     * @return the annotation names. May be {@code null} if there are none
     */
    public Set<String> getAnnotationsForField(ByteArrayKey fieldClass, Supplier<ByteArrayKey> fieldName) {
        Map<ByteArrayKey, Set<String>> fieldsInClass = fieldsWithAnnotations.get(fieldClass);
        if (fieldsInClass == null) {
            return null;
        }
        return fieldsInClass.get(fieldName.get());
    }

    /**
     * Gets the class name from the key for all classes contained in this index.
     * @param key the key
     * @return the classname. May be {@code null} if there are none
     */
    public String getClassNameFromKey(ByteArrayKey key) {
        return classNamesByKey.get(key);

    }

    /**
     * Gets the field name from the key for all fields contained in this index.
     * @param key the key
     * @return the field name. May be {@code null} if there are none
     */
    public String getFieldNameFromKey(ByteArrayKey key) {
        return fieldNamesByKey.get(key);
    }

    /**
     * Gets the method name from the key for all methods contained in this index.
     * @param key the key
     * @return the method name. May be {@code null} if there are none
     */
    public String getMethodNameFromKey(ByteArrayKey key) {
        return methodNamesByKey.get(key);
    }

    /**
     * Gets the method descriptor from the key for all methods contained in this index.
     * @param key the key
     * @return the method descriptor. May be {@code null} if there are none
     */
    public String getMethodDescriptorsFromKey(ByteArrayKey key) {
        return methodDescriptorsByKey.get(key);
    }

    /**
     * Gets the annotations on a class
     * @param superClassName the class to look for
     * @return the annotations. May be {@code null} if there are none
     */
    public Set<String> getAnnotationsForClass(String superClassName) {
        ByteArrayKey key = classKeysByName.get(superClassName);
        return getAnnotationsForClass(key);
    }

    /**
     * A key used for map lookup which takes an array and uses a subsection of that as the key value.
     * This reduces the need for creating new instances for each sub-array.
     */
    public static class ByteArrayKey {
        private final byte[] arr;
        private final int start;
        private final int length;

        private volatile int hash = 0;

        /**
         * Constructor
         *
         * @param arr the array to use as the key. The whole array will be used
         */
        private ByteArrayKey(byte[] arr) {
            this(arr, 0, arr.length);
        }


        private ByteArrayKey(byte[] arr, int start, int length) {
            if (arr == null) {
                throw new IllegalArgumentException("Null array");
            }
            this.arr = arr;
            this.start = start;
            this.length = length;

        }

        /**
         * Static factory method
         * @param arr the array to use as the key
         * @param start the first index of the array to use for lookups
         * @param length the length of the part of the array to use for lookups
         * @return the created key
         */
        public static ByteArrayKey create(byte[] arr, int start, int length) {
            return new ByteArrayKey(arr, start, length);
        }

        public int hashCode() {
            int hashCode = hash;
            if (hashCode == 0 && arr.length > 0) {
                hashCode = 1;
                int end = start + length;
                for (int i = start ; i < end ; i++) {
                    hashCode = 31 * hashCode + arr[i];
                }

                this.hash = hashCode;
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ByteArrayKey that = (ByteArrayKey) o;
            if (length != that.length || hashCode() != that.hashCode()) {
                return false;
            }
            return Arrays.equals(arr, start, start + length, that.arr, that.start, that.start + that.length);
        }

        /**
         * Converts the relevant bytes from this key to their string representation
         * @param reusableStreams factory to obtain reusable streams
         * @return the string from these bytes
         * @throws IOException
         */
        public String convertBytesToString(ReusableStreams reusableStreams) throws IOException {
            try (DataInputStream in = reusableStreams.getDataInputStream(arr, start, length)) {
                return in.readUTF();
            }
        }
    }
}
