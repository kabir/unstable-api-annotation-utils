package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.jboss.jandex.AdditionalScanInfoHook;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.ByteArrayKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JandexCollector implements AdditionalScanInfoHook {

    private final ByteRuntimeIndex runtimeIndex;

    private JandexClassInformation currentClass;

    private final Set<AnnotationUsage> usages = new LinkedHashSet<>();
    private final Set<AnnotationUsage> dotNameUsages = new LinkedHashSet<>();

    // TODO remove this, it is just here to diagnose errors
    public List<String> errorClasses = new ArrayList<>();

    public JandexCollector(ByteRuntimeIndex runtimeIndex) {
        this.runtimeIndex = runtimeIndex;
    }

    @Override
    public void startClass() {

    }


    @Override
    public void startConstantPool(int count) {
        currentClass = new JandexClassInformation(count);
    }

    @Override
    public void handleConstantPoolEntry(int pos, int tag, int offset, int length) {
        // The pos passed in here is zero based, while the indices are 1-based.
        // Adjust here
        pos++;

        switch (tag) {
            case BytecodeTags.CONSTANT_CLASS:
            case BytecodeTags.CONSTANT_FIELDREF:
            case BytecodeTags.CONSTANT_METHODREF:
            case BytecodeTags.CONSTANT_INTERFACEMETHODREF:
            case BytecodeTags.CONSTANT_NAMEANDTYPE:
            case BytecodeTags.CONSTANT_UTF8:
                currentClass.addConstantPoolEntry(pos, tag, offset, length);
                break;
            default:
                return;
        }

    }


    @Override
    public void setClassInfo(DotName thisName, Type superClassType, short flags, Type[] interfaceTypes) {
        currentClass.setThisName(thisName);
        currentClass.setSuperClassType(superClassType);
        currentClass.setInterfaceTypes(interfaceTypes);
    }

    @Override
    public void endConstantPool(byte[] constantPool) {
        currentClass.endConstantPool(constantPool);
    }

    public Set<AnnotationUsage> getUsages() {
        return usages;
    }

    @Override
    public void endClass() {
        //currentClass.getDebugUtils().outputRawFormat();

        try {
            // Class references might also come from extends/implements, so defer registering those until the end
            ClassReferences classReferences = new ClassReferences();

            for (int pos = 1 ; pos < currentClass.getTags().length ; pos++) {
                int tag = currentClass.getTags()[pos];
                switch (tag) {
                    case BytecodeTags.CONSTANT_INTERFACEMETHODREF:
                    case BytecodeTags.CONSTANT_METHODREF: {
                        final int constantPoolPosition = pos;
                        Set<String> annotations = runtimeIndex.getAnnotationsForMethod(
                                currentClass.getClassNameFromReference(constantPoolPosition),
                                () -> currentClass.getNameFromReference(constantPoolPosition),
                                () -> currentClass.getDescriptorFromReference(constantPoolPosition));
                        if (annotations != null) {
                            recordMethodUsage(annotations,
                                    currentClass.getClassNameFromReference(constantPoolPosition),
                                    currentClass.getNameFromReference(constantPoolPosition),
                                    currentClass.getDescriptorFromReference(constantPoolPosition));
                        }
                    }
                    break;
                    case BytecodeTags.CONSTANT_FIELDREF: {
                        final int constantPoolPosition = pos;
                        Set<String> annotations = runtimeIndex.getAnnotationsForField(
                                currentClass.getClassNameFromReference(constantPoolPosition),
                                () -> currentClass.getNameFromReference(constantPoolPosition));
                        if (annotations != null) {
                           recordFieldUsage(annotations,
                                   currentClass.getClassNameFromReference(constantPoolPosition),
                                   currentClass.getNameFromReference(constantPoolPosition));
                        }
                    }
                    break;
                    case BytecodeTags.CONSTANT_CLASS: {
                        ByteArrayKey key = currentClass.getClassNameFromClassInfo(pos);
                        Set<String> annotations = runtimeIndex.getAnnotationsForClass(key);
                        if (annotations != null) {
                            classReferences.classes.put(runtimeIndex.getClassNameFromKey(key), annotations);
                        }
                    }
                    break;
                }
            } // for

            // Now check the superclass and interfaces
            if (currentClass.getSuperClassType() != null && currentClass.getSuperClassType() != ClassType.OBJECT_TYPE) {
                String superClassName = currentClass.getSuperClassType().name().toString();
                Set<String> annotations = runtimeIndex.getAnnotationsForClass(superClassName);
                if (annotations != null) {
                    recordSuperClassUsage(annotations, superClassName);
                    classReferences.indirectReferences.add(superClassName);
                }
            }

            for (Type iface : currentClass.getInterfaceTypes()) {
                String ifaceName = iface.name().toString();
                Set<String> annotations = runtimeIndex.getAnnotationsForClass(ifaceName);
                if (annotations != null) {
                    recordImplementsInterfaceUsage(annotations, ifaceName);
                    classReferences.indirectReferences.add(ifaceName);
                }
            }

            classReferences.recordClassUsage(currentClass.getScannedClassName());
        } catch (RuntimeException e) {
            //System.err.println("Error in: "  + currentClass.getScannedClassName());
            // TODO swallowed exception
            errorClasses.add(currentClass.getScannedClassName());
            throw e;
        }
    }

    private void recordMethodUsage(Set<String> annotations, ByteArrayKey classNameFromReference, ByteArrayKey nameFromReference, ByteArrayKey descriptorFromReference) {
        AnnotatedMethodReference annotatedMethodReference = new AnnotatedMethodReference(
                annotations,
                currentClass.getScannedClassName(),
                runtimeIndex.getClassNameFromKey(classNameFromReference),
                runtimeIndex.getMethodNameFromKey(nameFromReference),
                runtimeIndex.getMethodDescriptorsFromKey(descriptorFromReference));
        usages.add(annotatedMethodReference);
    }

    private void recordFieldUsage(Set<String> annotations, ByteArrayKey classNameFromReference, ByteArrayKey nameFromReference) {
        AnnotatedFieldReference annotatedFieldReference = new AnnotatedFieldReference(
                annotations,
                currentClass.getScannedClassName(),
                runtimeIndex.getClassNameFromKey(classNameFromReference),
                runtimeIndex.getFieldNameFromKey(nameFromReference));
        usages.add(annotatedFieldReference);
    }

    private void recordImplementsInterfaceUsage(Set<String> annotations, String ifaceName) {
        usages.add(new ImplementsAnnotatedInterface(annotations, currentClass.getScannedClassName(), ifaceName));
    }

    private void recordSuperClassUsage(Set<String> annotations, String superClassName) {
        usages.add(new ExtendsAnnotatedClass(annotations, currentClass.getScannedClassName(), superClassName));
    }


    // For classes, we defer the recording of annotations since they may come indirectly from extends and implements etc.
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
