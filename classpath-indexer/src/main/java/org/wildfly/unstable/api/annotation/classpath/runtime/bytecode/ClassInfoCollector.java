package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.RecordComponentInfo;
import org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Takes a parsed {@link ClassInformation} and checks it against the {@code RuntimeIndex}, recording
 * any usage of indexed classes/members as {@link AnnotationUsage} instances.
 */
class ClassInfoCollector {
    private final RuntimeIndex runtimeIndex;

    private final ReusableStreams reusableStreams = new ReusableStreams();

    private final Set<AnnotationUsage> usages = new LinkedHashSet<>();

    ClassInfoCollector(RuntimeIndex runtimeIndex) {
        this.runtimeIndex = runtimeIndex;
    }

    /**
     * Gets the usages of annotations
     * @return the usages
     */
    Set<AnnotationUsage> getUsages() {
        return usages;
    }

    /**
     * Takes a parsed {@link ClassInformation} and checks it against the {@code RuntimeIndex}, recording
     * any usage of indexed classes/members as {@link AnnotationUsage} instances.
     */
    void processClass(ClassInformation classInfo) throws IOException {
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
                    RuntimeIndex.ByteArrayKey key = classInfo.getClassNameFromClassInfo(pos);
                    Set<String> annotations = runtimeIndex.getAnnotationsForClass(key);
                    if (annotations != null) {
                        classReferences.classes.put(runtimeIndex.getClassNameFromKey(key), annotations);
                    }
                }
                case BytecodeTags.CONSTANT_METHODHANDLE: {
                    // We don't need to do anything! (I originally thought we did)
                    // Basically this will point to a CONSTANT_FieldRef_Info, CONSTANT_MethodRed_Info or a
                    // CONSTANT_InterfaceMethodRef_Info, which will all be inspected anyway in their relevant
                    // case statements above.
                }
                break;
            }
        }


        // Now check the superclass and interfaces
        RuntimeIndex.ByteArrayKey superClass = classInfo.getSuperClass();
        if (superClass != null && !RuntimeIndex.JAVA_LANG_OBJECT_KEY.equals(superClass)) {

            Set<String> annotations = runtimeIndex.getAnnotationsForClass(superClass);
            if (annotations != null) {
                // This is only called once, no need to cache in classInfo
                String superClassName = RuntimeIndex.convertClassNameToDotFormat(superClass.convertBytesToString(reusableStreams));
                recordSuperClassUsage(classInfo, annotations, superClassName);
                classReferences.indirectReferences.add(superClassName);
            }
        }

        for (RuntimeIndex.ByteArrayKey iface : classInfo.getInterfaces()) {
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(iface);
            if (annotations != null) {
                // This is only called once, no need to cache in classInfo
                String ifaceName = RuntimeIndex.convertClassNameToDotFormat(iface.convertBytesToString(reusableStreams));
                recordImplementsInterfaceUsage(classInfo, annotations, ifaceName);
                classReferences.indirectReferences.add(ifaceName);
            }
        }

        classReferences.recordClassUsage(classInfo.getScannedClassName(reusableStreams));
    }

    boolean checkAnnotationIndex(JandexIndex annotationIndex) {
        return new AnnotationIndexChecker(annotationIndex).checkAnnotationIndex();
    }

    private void recordMethodUsage(ClassInformation classInfo, Set<String> annotations, RuntimeIndex.ByteArrayKey classNameFromReference, RuntimeIndex.ByteArrayKey nameFromReference, RuntimeIndex.ByteArrayKey descriptorFromReference) throws IOException {
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

    private void recordFieldUsage(ClassInformation classInfo, Set<String> annotations, RuntimeIndex.ByteArrayKey classNameFromReference, RuntimeIndex.ByteArrayKey nameFromReference) throws IOException {
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

    private class AnnotationIndexChecker {
        private final JandexIndex annotationIndex;

        public AnnotationIndexChecker(JandexIndex annotationIndex) {
            this.annotationIndex = annotationIndex;
        }

        public boolean checkAnnotationIndex() {
            Map<AnnotationTarget, Set<String>> annotationsByTarget = new HashMap<>();
            Set<RecordComponentInfo> recordComponents = new HashSet<>();
            for (String annotation : runtimeIndex.getAnnotatedAnnotations()) {
                Collection<AnnotationInstance> annotationInstances = annotationIndex.getAnnotations(annotation);
                for (AnnotationInstance instance : annotationInstances) {
                    AnnotationTarget target = instance.target();
                    addAnnotationTarget(annotationsByTarget, recordComponents, annotation, target);
                }
            }

            trimRecordComponent(annotationsByTarget, recordComponents);
            processAnnotationTargets(annotationsByTarget);
            return annotationsByTarget.isEmpty();
        }

        private void addAnnotationTarget(Map<AnnotationTarget, Set<String>> annotationsByTarget,
                                         Set<RecordComponentInfo> recordComponents, String annotation, AnnotationTarget target) {
            if (target.kind() == AnnotationTarget.Kind.TYPE) {
                addAnnotationTarget(annotationsByTarget, recordComponents, annotation, target.asType().enclosingTarget());
                return;
            }
            if (target.kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                MethodParameterInfo minfo = target.asMethodParameter();
                target = minfo.method();
            } else if (target.kind() == AnnotationTarget.Kind.RECORD_COMPONENT) {
                recordComponents.add(target.asRecordComponent());
            }
            Set<String> annotations = annotationsByTarget.computeIfAbsent(target, k -> new HashSet<>());
            annotations.add(annotation);
        }

        private void trimRecordComponent(Map<AnnotationTarget, Set<String>> annotationsByTarget, Set<RecordComponentInfo> recordComponents) {
            for (RecordComponentInfo recordComponent : recordComponents) {
                annotationsByTarget.remove(recordComponent.accessor());
                annotationsByTarget.remove(recordComponent.field());
            }
        }

        private boolean processAnnotationTargets(Map<AnnotationTarget, Set<String>> annotationsByTarget) {
            Set<AnnotationUsage> annotationUsages = new HashSet<>();
            for (AnnotationTarget target : annotationsByTarget.keySet()) {
                Set<String> annotations = annotationsByTarget.get(target);
                AnnotationTarget.Kind kind = target.kind();
                if (kind == AnnotationTarget.Kind.CLASS) {
                    ClassInfo classInfo = target.asClass();
                    annotationUsages.add(new AnnotationOnUserClassUsage(classInfo.name().toString(), annotations));

                } else if (kind == AnnotationTarget.Kind.FIELD) {
                    FieldInfo fieldInfo = target.asField();
                    String className = fieldInfo.declaringClass().name().toString();
                    String fieldName = fieldInfo.name();
                    annotationUsages.add(new AnnotationOnUserFieldUsage(className, fieldName, annotations));
                } else if (kind == AnnotationTarget.Kind.METHOD) {
                    MethodInfo methodInfo = target.asMethod();
                    String className = methodInfo.declaringClass().name().toString();
                    String methodName = methodInfo.name();
                    String desc = methodInfo.descriptor();
                    annotationUsages.add(new AnnotationOnUserMethodUsage(className, methodName, desc, annotations));
                } else if (kind == AnnotationTarget.Kind.TYPE) {
                    // This should not happen. We are getting the enclosing target for this case in addAnnotationTarget
                    throw new IllegalStateException();
                } else if (kind == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    // This should not happen. We are getting the method containing the method in addAnnotationTarget
                } else if (kind == AnnotationTarget.Kind.RECORD_COMPONENT) {

                }
            }
            ClassInfoCollector.this.usages.addAll(annotationUsages);
            return annotationUsages.isEmpty();
        }
    }

}
