package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.jboss.jandex.AnnotationInstance;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.ByteArrayKey;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.JAVA_LANG_OBJECT_KEY;
import static org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.convertClassNameToDotFormat;

class ClassInfoCollector {
    private final ByteRuntimeIndex runtimeIndex;

    private final ReusableStreams reusableStreams = new ReusableStreams();

    private final Set<AnnotationUsage> usages = new LinkedHashSet<>();

    ClassInfoCollector(ByteRuntimeIndex runtimeIndex) {
        this.runtimeIndex = runtimeIndex;
    }

    public Set<AnnotationUsage> getUsages() {
        return usages;
    }

    public void processClass(ClassInformation classInfo) throws IOException {
        ClassReferences classReferences = new ClassReferences();
        int[] tags = classInfo.getTags();
        for (int i = 0; i < tags.length; i++) {
            // Our arrays are zero based, while the indices referred to by the bytecode are one based
            int pos = i + 1;
            int tag = tags[i];
            switch (tag) {
                case BytecodeTags.CONSTANT_FIELDREF:{
                    Set<String> annotations = runtimeIndex.getAnnotationsForField(
                            classInfo.getClassNameFromRefInfo(pos),
                            () -> classInfo.getNameFromRefInfo(pos));
                    if (annotations != null) {
                        recordFieldUsage(
                                classInfo,
                                annotations,
                                classInfo.getClassNameFromRefInfo(pos),
                                classInfo.getNameFromRefInfo(pos));
                    }
                }
                break;
                case BytecodeTags.CONSTANT_METHODREF:
                case BytecodeTags.CONSTANT_INTERFACEMETHODREF: {

                    Set<String> annotations = runtimeIndex.getAnnotationsForMethod(
                            classInfo.getClassNameFromRefInfo(pos),
                            () -> classInfo.getNameFromRefInfo(pos),
                            () -> classInfo.getDescriptorFromRefInfo(pos));
                    if (annotations != null) {
                        recordMethodUsage(
                                classInfo,
                                annotations,
                                classInfo.getClassNameFromRefInfo(pos),
                                classInfo.getNameFromRefInfo(pos),
                                classInfo.getDescriptorFromRefInfo(pos));
                    }
                }
                break;
                case BytecodeTags.CONSTANT_CLASS: {
                    ByteArrayKey key = classInfo.getClassNameFromClassInfo(pos);
                    Set<String> annotations = runtimeIndex.getAnnotationsForClass(key);
                    if (annotations != null) {
                        classReferences.classes.put(runtimeIndex.getClassNameFromKey(key), annotations);
                    }
                }
                break;
            }
        }


        // Now check the superclass and interfaces
        ByteArrayKey superClass = classInfo.getSuperClass();
        if (superClass != null && !JAVA_LANG_OBJECT_KEY.equals(superClass)) {

            Set<String> annotations = runtimeIndex.getAnnotationsForClass(superClass);
            if (annotations != null) {
                // This is only called once, no need to cache in classInfo
                String superClassName = convertClassNameToDotFormat(superClass.convertBytesToString(reusableStreams));
                recordSuperClassUsage(classInfo, annotations, superClassName);
                classReferences.indirectReferences.add(superClassName);
            }
        }

        for (ByteArrayKey iface : classInfo.getInterfaces()) {
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(iface);
            if (annotations != null) {
                // This is only called once, no need to cache in classInfo
                String ifaceName = convertClassNameToDotFormat(iface.convertBytesToString(reusableStreams));
                recordImplementsInterfaceUsage(classInfo, annotations, ifaceName);
                classReferences.indirectReferences.add(ifaceName);
            }
        }

        classReferences.recordClassUsage(classInfo.getScannedClassName(reusableStreams));
    }

    public boolean checkAnnotationIndex(JandexIndex annotationIndex) {
        boolean noAnnotationUsage = true;
        Set<String> annotations = new HashSet<>();
        for (String annotation : runtimeIndex.getAnnotatedAnnotations()) {
            Collection<AnnotationInstance> annotationInstances =  annotationIndex.getAnnotations(annotation);
            if (!annotationInstances.isEmpty()) {
                // TODO it would be good to record WHERE the annotations are referenced from. At least the class where it happens.
                // But I am not sure about what some of the 'Kind's returned by AnnotationInstance.target().kind are at the moment.
                // The ones I am unsure about are Kind.TYPE and Kind.RECORD_COMPONENT
                annotations.add(annotation);
                noAnnotationUsage = false;
            }
        }
        if (annotations.size() > 0) {
            usages.add(new AnnotatedAnnotation(annotations));
        }
        return noAnnotationUsage;
    }

    private void recordMethodUsage(ClassInformation classInfo, Set<String> annotations, ByteArrayKey classNameFromReference, ByteArrayKey nameFromReference, ByteArrayKey descriptorFromReference) throws IOException {
        //The name of the scanned class will not be in the index, so we need to get that separately
        String scannedClass = classInfo.getScannedClassName(reusableStreams);

        AnnotatedMethodReference annotatedMethodReference = new AnnotatedMethodReference(
                annotations,
                scannedClass,
                runtimeIndex.getClassNameFromKey(classNameFromReference),
                runtimeIndex.getMethodNameFromKey(nameFromReference),
                runtimeIndex.getMethodDescriptorsFromKey(descriptorFromReference));
        usages.add(annotatedMethodReference);
    }

    private void recordFieldUsage(ClassInformation classInfo, Set<String> annotations, ByteArrayKey classNameFromReference, ByteArrayKey nameFromReference) throws IOException {
        //The name of the scanned class will not be in the index, so we need to get that separately
        String scannedClass = classInfo.getScannedClassName(reusableStreams);

        AnnotatedFieldReference annotatedFieldReference = new AnnotatedFieldReference(
                annotations,
                scannedClass,
                runtimeIndex.getClassNameFromKey(classNameFromReference),
                runtimeIndex.getFieldNameFromKey(nameFromReference));
        usages.add(annotatedFieldReference);
    }

    private void recordImplementsInterfaceUsage(ClassInformation classInfo, Set<String> annotations, String ifaceName) throws IOException {
        //The name of the scanned class will not be in the index, so we need to get that separately
        String scannedClass = classInfo.getScannedClassName(reusableStreams);
        usages.add(new ImplementsAnnotatedInterface(annotations, scannedClass, ifaceName));
    }

    private void recordSuperClassUsage(ClassInformation classInfo, Set<String> annotations, String superClassName) throws IOException {
        //The name of the scanned class will not be in the index, so we need to get that separately
        String scannedClass = classInfo.getScannedClassName(reusableStreams);
        usages.add(new ExtendsAnnotatedClass(annotations, scannedClass, superClassName));
    }


    // For classes, we defer the recording of annotations since they may come indirectly
    // from extends and implements etc.
    private class ClassReferences {
        // Classes referenced by extends/implements etc.
        private final Set<String> indirectReferences = new HashSet<>();
        // Annotations for class references
        private final Map<String, Set<String>> classes = new HashMap<>();

        boolean recordClassUsage(String className) {
            boolean empty = true;
            for (String s : indirectReferences) {
                classes.remove(s);
            }
            for (String referencedClass : classes.keySet()) {
                usages.add(new AnnotatedClassUsage(classes.get(referencedClass), className, referencedClass));
                empty = false;
            }
            return empty;
        }
    }
}
