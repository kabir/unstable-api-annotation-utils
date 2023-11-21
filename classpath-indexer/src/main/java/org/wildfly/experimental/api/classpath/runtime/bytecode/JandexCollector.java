package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.jboss.jandex.AdditionalScanInfoHook;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.ByteArrayKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JandexCollector implements AdditionalScanInfoHook {

    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref = 11;
    private static final int CONSTANT_NameAndType = 12;

    // We might be interested in these?
    //private static final int CONSTANT_MethodHandle = 15;
    //private static final int CONSTANT_MethodType = 16;
    //private static final int CONSTANT_Dynamic = 17;
    //private static final int CONSTANT_InvokeDynamic = 18;

    private final ByteRuntimeIndex runtimeIndex;

    private ClassInformation currentClass;

    private final Set<AnnotationUsage> usages = new LinkedHashSet<>();
    private final Set<AnnotationUsage> dotNameUsages = new LinkedHashSet<>();

    public JandexCollector(ByteRuntimeIndex runtimeIndex) {
        this.runtimeIndex = runtimeIndex;
    }

    @Override
    public void startClass() {

    }


    @Override
    public void startConstantPool(int count) {
        currentClass = new ClassInformation(count);
    }

    @Override
    public void handleConstantPoolEntry(int pos, int tag, int offset, int length) {
        // The pos passed in here is zero based, while the indices are 1-based.
        // Adjust here
        pos++;

        switch (tag) {
            case CONSTANT_Class:
            case CONSTANT_Fieldref:
            case CONSTANT_Methodref:
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_NameAndType:
            case CONSTANT_Utf8:
                currentClass.addConstantPoolEntry(pos, tag, offset, length);
                break;
            default:
                return;
        }

    }


    @Override
    public void setClassInfo(DotName thisName, Type superClassType, short flags, Type[] interfaceTypes) {
        currentClass.thisName = thisName;
        currentClass.superClassType = superClassType;
        currentClass.interfaceTypes = interfaceTypes;
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
        // Class references might also come from extends/implements, so defer registering those until the end
        ClassReferences classReferences = new ClassReferences();

        for (int pos = 1 ; pos < currentClass.tags.length ; pos++) {
            int tag = currentClass.tags[pos];
            switch (tag) {
                case CONSTANT_InterfaceMethodref:
                case CONSTANT_Methodref: {
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
                case CONSTANT_Fieldref: {
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
                case CONSTANT_Class: {
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
        if (currentClass.superClassType != ClassType.OBJECT_TYPE) {
            String superClassName = currentClass.superClassType.name().toString();
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(superClassName);
            if (annotations != null) {
                recordSuperClassUsage(annotations, superClassName);
                classReferences.indirectReferences.add(superClassName);
            }
        }

        for (Type iface : currentClass.interfaceTypes) {
            String ifaceName = iface.name().toString();
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(ifaceName);
            if (annotations != null) {
                recordImplementsInterfaceUsage(annotations, ifaceName);
                classReferences.indirectReferences.add(ifaceName);
            }
        }

        classReferences.recordClassUsage(currentClass.getScannedClassName());
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

    private static class ClassInformation {
        private final int[] tags;
        private final int[] offsets;
        private final int[] lengths;

        // Cache the ByteArrayKeys read at the corresponding ClassInfo entry locations
        private final ByteArrayKey[] byteArrayKeys;

        private byte[] constantPool;

        private DotName thisName;
        private String thisNameStr;
        private Type superClassType;
        private Type[] interfaceTypes;


        ClassInformation(int constantPoolSize) {
            tags = new int[constantPoolSize + 1];
            offsets = new int[constantPoolSize + 1];
            lengths = new int[constantPoolSize + 1];
            byteArrayKeys = new ByteArrayKey[constantPoolSize + 1];
        }

        void addConstantPoolEntry(int pos, int tag, int offset, int length) {
            tags[pos] = tag;
            offsets[pos] = offset;
            lengths[pos] = length;
        }

        void setClassInfo(DotName thisName, Type superClassType, short flags, Type[] interfaceTypes) {
            this.thisName = thisName;
            this.superClassType = superClassType;
            this.interfaceTypes = interfaceTypes;
        }


        void endConstantPool(byte[] constantPool) {
            this.constantPool = constantPool;
        }

        /**
         * FieldRefInfo, MethodRedInfo and InterfaceRefInfo all have the same structure.
         * This method gets the value of the first entry
         * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
         * @return the name of the class containing the reference
         */
        ByteArrayKey getClassNameFromReference(int constantPoolPosition) {
            // The location of the ClassInfo will be the first two bytes
            int classPosition = readUnsignedShort(offsets[constantPoolPosition]);
            // Get the name of the class
            return getClassNameFromClassInfo(classPosition);
        }

        /**
         * FieldRefInfo, MethodRedInfo and InterfaceRefInfo all have the same structure.
         * This method finds the NameAndTypeInfo from the XXX RedInfo. In the NameAndTypeInfo
         * the index of the UTFInfo containing the name will be stored in the first two bytes.
         * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
         * @return the name of the field/method pointed at by the XXXRefInfo
         */
        ByteArrayKey getNameFromReference(int constantPoolPosition) {
            int nameAndTypeInfoOffset = getNameAndTypeInfoOffsetFromReference(constantPoolPosition);
            // The location of the NameAndTypeInfo will be its fist two bytes
            int refNamePosition = readUnsignedShort(nameAndTypeInfoOffset);
            return getKeyFromUtfInfo(refNamePosition);
        }

        /**
         * FieldRefInfo, MethodRedInfo and InterfaceRefInfo all have the same structure.
         * This method finds the NameAndTypeInfo from the XXX RedInfo. In the NameAndTypeInfo
         * the index of the UTFInfo containing the descriptor will be stored in the third and fourth bytes.
         * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
         * @return the name of the field/method pointed at by the XXXRefInfo
         */
        ByteArrayKey getDescriptorFromReference(int constantPoolPosition) {
            int nameAndTypeInfoOffset = getNameAndTypeInfoOffsetFromReference(constantPoolPosition);
            // The location of the NameAndTypeInfo will be its third and fourth bytes
            int refNamePosition = readUnsignedShort(nameAndTypeInfoOffset + 2);
            return getKeyFromUtfInfo(refNamePosition);
        }

        /**
         * FieldRefInfo, MethodRedInfo and InterfaceRefInfo all have the same structure.
         * This method gets the position of where their NameAndTypeInfo is stored
         *
         * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
         * @return the name of the class containing the reference
         */
        private int getNameAndTypeInfoOffsetFromReference(int constantPoolPosition) {
            // The location of the nameandtypeinfo will be the second two bytes
            int offset = offsets[constantPoolPosition] + 2;
            int position = readUnsignedShort(offset);
            return offsets[position];
        }

        private ByteArrayKey getClassNameFromClassInfo(int constantPoolPosition) {
            ByteArrayKey key = byteArrayKeys[constantPoolPosition];
            if (key == null) {
                // ClassInfo just contains the location of the UtfInfo containing the class bane
                int utfInfoPosition = readUnsignedShort(offsets[constantPoolPosition]);
                key = getKeyFromUtfInfo(utfInfoPosition);
                byteArrayKeys[constantPoolPosition] = key;
            }
            return key;
        }

        private ByteArrayKey getKeyFromUtfInfo(int constantPoolPosition) {
            ByteArrayKey key = byteArrayKeys[constantPoolPosition];
            if (key == null) {
                key = ByteArrayKey.create(constantPool, offsets[constantPoolPosition], lengths[constantPoolPosition]);
                byteArrayKeys[constantPoolPosition] = key;
            }
            return key;
        }

        // Indexes in the array are stored as two bytes, and interpreted as unsigned shorts
        // This replicates how DataInputStream reads unsigned shorts
        private int readUnsignedShort(int offset) {
            int ch1 = constantPool[offset];
            int ch2 = constantPool[offset + 1];
            if ((ch1 | ch2) < 0)
                throw new IllegalStateException();
            return (ch1 << 8) + (ch2 << 0);
        }

        String getScannedClassName() {
            if (thisNameStr == null) {
                thisNameStr = thisName.toString();
            }
            return thisNameStr;
        }
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
