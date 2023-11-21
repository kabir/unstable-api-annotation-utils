package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.jboss.jandex.AnnotationInstance;
import org.wildfly.experimental.api.classpath.index.RuntimeIndex;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ConstantPool.AbstractRefInfo;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ConstantPool.ClassInfo;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ConstantPool.NameAndTypeInfo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ClassBytecodeInspector {

    public static final String BYTECODE_CONSTRUCTOR_NAME = RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME;

    private final RuntimeIndex runtimeIndex;

    private final Set<AnnotationUsage> usages = new LinkedHashSet<>();
    private final Set<AnnotationUsage> dotNameUsages = new LinkedHashSet<>();


    public ClassBytecodeInspector(RuntimeIndex runtimeIndex) {
        this.runtimeIndex = runtimeIndex;
    }

    public Set<AnnotationUsage> getUsages() {
        if (usages.size() != dotNameUsages.size()) {
            for (AnnotationUsage usage : this.usages) {
                dotNameUsages.add(usage.convertToDotFormat());
            }
        }
        return dotNameUsages;
    }

    /**
     * Scans a class file and looks for usage of things annotated by the experimental annotations
     *
     * @param classInputStream An input stream with the class bytes. A plain input stream may be used. This method will
     *                         wrap in a BufferedInputStream
     * @return {@code true} if no usage was found
     * @throws IOException
     */
    public boolean scanClassFile(InputStream classInputStream) throws IOException {
        boolean noAnnotationUsage = true;
        DataInputStream in = new DataInputStream(new BufferedInputStream(classInputStream));

        // Parse the stuff before the ConstantPool
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("Not a valid class file (no CAFEBABE header)");
        }
        //Minor Version, we don't need this
        in.readUnsignedShort();
        // Major version, we don't need this
        in.readUnsignedShort();

        ////////////////////////////////////
        // Check the constant pool for method (includes constructors) and field references
        ConstantPool constantPool = ConstantPool.read(in);


        //////////////////////////////////////////
        // Read the class, superclass and interfaces

        // Class references might also come from extends/implements, so defer registering those until the end
        ClassReferences classReferences = new ClassReferences();

        // Access flags, we don't need this
        int access_flags = in.readUnsignedShort();

        // The name of this class
        String scannedClass = constantPool.className(in.readUnsignedShort());

        String superClass = null;
        int super_class_index = in.readUnsignedShort();
        if (super_class_index != 0) {
            superClass = constantPool.className(super_class_index);
        }

        Set<String> interfaces = new HashSet<>();
        int interfacesCount = in.readUnsignedShort();
        for (int i = 0; i < interfacesCount; i++) {
            int interfaceIndex = in.readUnsignedShort();
            String iface = constantPool.className(interfaceIndex);
            interfaces.add(iface);
        }

        ////////////////////////////////////////////
        // Now look for references in the parsed class pool

        for (ConstantPool.Info info : constantPool.pool) {
            if (info == null) {
                continue;
            }
            switch (info.tag()) {
                case ConstantPool.CONSTANT_Fieldref:
                case ConstantPool.CONSTANT_InterfaceMethodref:
                case ConstantPool.CONSTANT_Methodref: {
                    AbstractRefInfo ref = (AbstractRefInfo)info;
                    String declaringClass = constantPool.className(ref.class_index);
                    NameAndTypeInfo nameAndTypeInfo = (NameAndTypeInfo)constantPool.pool[ref.name_and_type_index];
                    String refName = constantPool.utf8(nameAndTypeInfo.name_index);
                    if (info.tag() == ConstantPool.CONSTANT_Fieldref) {
                        Set<String> annotations = runtimeIndex.getAnnotationsForField(declaringClass, refName);
                        if (annotations != null) {
                            recordFieldUsage(annotations, scannedClass, declaringClass, refName);
                            noAnnotationUsage = false;
                        }
                    } else {
                        String descriptor = constantPool.utf8(nameAndTypeInfo.descriptor_index);
                        Set<String> annotations = runtimeIndex.getAnnotationsForMethod(declaringClass, refName, descriptor);
                        if (annotations != null) {
                            recordMethodUsage(annotations, scannedClass, declaringClass, refName, descriptor);
                            noAnnotationUsage = false;
                        }
                    }

                    break;
                }
                case ConstantPool.CONSTANT_Class:
                    ClassInfo classInfo = (ClassInfo) info;
                    String otherClass = constantPool.utf8(classInfo.class_index);
                    Set<String> annotations = runtimeIndex.getAnnotationsForClass(otherClass);
                    if (annotations != null) {
                        // Don't record this right away, since class usage might come from the other ways of referencing
                        classReferences.classes.put(otherClass, annotations);
                    }
                // TODO might need to look into MethodHandle etc.
            }
        }

        // Now check the super class and interfaces
        if (superClass != null) {
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(superClass);
            if (annotations != null) {
                recordSuperClassUsage(annotations, scannedClass, superClass);
                classReferences.indirectReferences.add(superClass);
                noAnnotationUsage = false;
            }
        }


        for (String iface : interfaces) {
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(iface);
            if (annotations != null) {
                recordImplementsInterfaceUsage(annotations, scannedClass, iface);
                classReferences.indirectReferences.add(iface);
                noAnnotationUsage = false;
            }
        }

        noAnnotationUsage = classReferences.recordClassUsage(scannedClass) && noAnnotationUsage;



        return noAnnotationUsage;
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

    private void recordSuperClassUsage(Set<String> annotations, String clazz, String superClass) {
        usages.add(new ExtendsAnnotatedClass(annotations, clazz, superClass));
    }

    private void recordImplementsInterfaceUsage(Set<String> annotations, String className, String iface) {
        usages.add(new ImplementsAnnotatedInterface(annotations, className, iface));
    }

    private void recordFieldUsage(Set<String> annotations, String className, String fieldClass, String fieldName) {
        usages.add(new AnnotatedFieldReference(annotations, className, fieldClass, fieldName));
    }

    private void recordMethodUsage(Set<String> annotations, String className, String methodClass, String methodName, String descriptor) {
        usages.add(new AnnotatedMethodReference(annotations, className, methodClass, methodName, descriptor));
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
