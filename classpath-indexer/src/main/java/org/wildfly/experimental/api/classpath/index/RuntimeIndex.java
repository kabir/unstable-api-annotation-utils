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

    public static final ByteArrayKey JAVA_LANG_OBJECT_KEY = ByteArrayKey.create(OBJECT_BYTES, 0, OBJECT_BYTES.length);

    private static final ByteArrayOutputStream BYTE_ARRAY_OUTPUT_STREAM = new ByteArrayOutputStream(2048);


    public static final ByteArrayKey BYTECODE_CONSTRUCTOR_KEY;
    static {
        try {
            BYTECODE_CONSTRUCTOR_KEY = convertStringToByteArrayKey(BYTECODE_CONSTRUCTOR_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Classes, interfaces and annotations, and their annotations
    // We are including annotations here since users might decide to implement an annotation interface
    // that is marked as experimental
    private final Map<ByteArrayKey, Set<String>> allClassesWithAnnotations;

    // Annotations with annotations. Although these are also part of allClassesWithAnnotations,
    // this field will be needed as input to the Jandex scanning for annotation usage
    private final Map<String, Set<String>> annotationsWithAnnotations;


    // Keys in 'nested order' are className, methodname, descriptor. The set is the annotations for the method.
    private final Map<ByteArrayKey, Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>>> methodsWithAnnotations;

    // Keys in 'nested order' are className, fieldName. The set is the annotations for the method.
    private final Map<ByteArrayKey, Map<ByteArrayKey, Set<String>>> fieldsWithAnnotations;


    private final Map<ByteArrayKey, String> classNamesByKey;
    private final Map<String, ByteArrayKey> classKeysByName;
    private final Map<ByteArrayKey, String> methodNamesByKey;
    private final Map<ByteArrayKey, String> fieldNamesByKey;
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

    public static RuntimeIndex load(Path indexFile, Path... additional) throws IOException {
        OverallIndex overallIndex = OverallIndex.load(indexFile, additional);
        return convertOverallIndexToRuntimeIndex(overallIndex);
    }

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

    public static String convertClassNameToVmFormat(String s) {
        return s.replaceAll("\\.", "/");
    }

    public static String convertClassNameToDotFormat(String s) {
        return s.replaceAll("/", ".");
    }

    public Set<String> getAnnotationsForClass(ByteArrayKey key) {
        return allClassesWithAnnotations.get(key);
    }

    public Set<String> getAnnotationsForAnnotation(String annotation) {
        return annotationsWithAnnotations.get(annotation);
    }

    public Set<String> getAnnotatedAnnotations() {
        return annotationsWithAnnotations.keySet();
    }

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

    public Set<String> getAnnotationsForField(ByteArrayKey fieldClass, Supplier<ByteArrayKey> fieldName) {
        Map<ByteArrayKey, Set<String>> fieldsInClass = fieldsWithAnnotations.get(fieldClass);
        if (fieldsInClass == null) {
            return null;
        }
        return fieldsInClass.get(fieldName.get());
    }

    public String getClassNameFromKey(ByteArrayKey key) {
        return classNamesByKey.get(key);

    }

    public String getFieldNameFromKey(ByteArrayKey key) {
        return fieldNamesByKey.get(key);
    }

    public String getMethodNameFromKey(ByteArrayKey key) {
        return methodNamesByKey.get(key);
    }

    public String getMethodDescriptorsFromKey(ByteArrayKey key) {
        return methodDescriptorsByKey.get(key);
    }

    public Set<String> getAnnotationsForClass(String superClassName) {
        ByteArrayKey key = classKeysByName.get(superClassName);
        return getAnnotationsForClass(key);
    }

    public static class ByteArrayKey {
        private final byte[] arr;
        private final int start;
        private final int length;

        private volatile int hash = 0;

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

        public String convertBytesToString(ReusableStreams reusableStreams) throws IOException {
            try (DataInputStream in = reusableStreams.getDataInputStream(arr, start, length)) {
                return in.readUTF();
            }
        }
    }
}
