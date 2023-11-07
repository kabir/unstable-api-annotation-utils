package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
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

import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.ANNOTATION_USAGE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.CLASS_USAGE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.EXTENDS_CLASS;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.FIELD_REFERENCE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.IMPLEMENTS_INTERFACE;
import static org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector.AnnotationUsageType.METHOD_REFERENCE;

public class ClassBytecodeInspector {

    public static final String BYTECODE_CONSTRUCTOR_NAME = RuntimeIndex.BYTECODE_CONSTRUCTOR_NAME;

    private final RuntimeIndex runtimeIndex;

    private final Set<AnnotationUsage> usages = new LinkedHashSet<>();


    public ClassBytecodeInspector(RuntimeIndex runtimeIndex) {
        this.runtimeIndex = runtimeIndex;
    }

    public Set<AnnotationUsage> getUsages() {
        return usages;
    }

    /**
     * Scans a class file and looks for usage of things annotated by the experimental annotations
     *
     * @param className The name of the class we are scanning
     * @param classInputStream An input stream with the class bytes. A plain input stream may be used. This method will
     *                         wrap in a BufferedInputStream
     * @return {@code true} if no usage was found
     * @throws IOException
     */
    public boolean scanClassFile(String className, InputStream classInputStream) throws IOException {
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

        // Class references might also come from extends/implements, so defer registering those until the end
        ClassReferences classReferences = new ClassReferences();

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
                            recordFieldUsage(annotations, className, declaringClass, refName);
                            noAnnotationUsage = false;
                        }
                    } else {
                        String descriptor = constantPool.utf8(nameAndTypeInfo.descriptor_index);
                        Set<String> annotations = runtimeIndex.getAnnotationsForMethod(declaringClass, refName, descriptor);
                        if (annotations != null) {
                            recordMethodUsage(annotations, className, declaringClass, refName, descriptor);
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

        //////////////////////////////////////////
        // Read and check the superclass and interfaces

        // Access flags, we don't need this
        int access_flags = in.readUnsignedShort();

        // This class index, we don't need this
        in.readUnsignedShort();

        int super_class_index = in.readUnsignedShort();
        if (super_class_index != 0) {
            String superClass = constantPool.className(super_class_index);
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(superClass);
            if (annotations != null) {
                recordSuperClassUsage(annotations, className, superClass);
                classReferences.indirectReferences.add(superClass);
                noAnnotationUsage = false;
            }
        }


        int interfacesCount = in.readUnsignedShort();
        for (int i = 0; i < interfacesCount; i++) {
            int interfaceIndex = in.readUnsignedShort();
            String iface = constantPool.className(interfaceIndex);
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(iface);
            if (annotations != null) {
                recordImplementsInterfaceUsage(annotations, className, iface);
                classReferences.indirectReferences.add(iface);
                noAnnotationUsage = false;
            }
        }

        noAnnotationUsage = noAnnotationUsage && classReferences.recordClassUsage(className);

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

    private void recordAnnotationUsage(Set<String> annotations) {

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

    public static abstract class AnnotationUsage {
        private final Set<String> annotations;
        private final AnnotationUsageType type;

        public AnnotationUsage(Set<String> annotations, AnnotationUsageType type) {
            this.annotations = annotations;
            this.type = type;
        }

        public AnnotationUsageType getType() {
            return type;
        }

        public Set<String> getAnnotations() {
            return annotations;
        }

        public ExtendsAnnotatedClass asExtendsAnnotatedClass() {
            if (type != EXTENDS_CLASS) {
                throw new IllegalStateException();
            }
            return (ExtendsAnnotatedClass) this;
        }

        public ImplementsAnnotatedInterface asImplementsAnnotatedInterface() {
            if (type != IMPLEMENTS_INTERFACE) {
                throw new IllegalStateException();
            }
            return (ImplementsAnnotatedInterface) this;
        }

        public AnnotatedMethodReference asAnnotatedMethodReference() {
            if (type != METHOD_REFERENCE) {
                throw new IllegalStateException();
            }
            return (AnnotatedMethodReference) this;
        }

        public AnnotatedFieldReference asAnnotatedFieldReference() {
            if (type != FIELD_REFERENCE) {
                throw new IllegalStateException();
            }
            return (AnnotatedFieldReference) this;
        }

        public AnnotatedClassUsage asAnnotatedClassUsage() {
            if (type != CLASS_USAGE) {
                throw new IllegalStateException();
            }
            return (AnnotatedClassUsage) this;
        }
        public AnnotatedAnnotation asAnnotatedAnnotation() {
            if (type != ANNOTATION_USAGE) {
                throw new IllegalStateException();
            }
            return (AnnotatedAnnotation) this;
        }
    }

    public static class AnnotationWithSourceClassUsage extends AnnotationUsage {
        private final String sourceClass;

        public AnnotationWithSourceClassUsage(Set<String> annotations, AnnotationUsageType type, String sourceClass) {
            super(annotations, type);
            this.sourceClass = RuntimeIndex.convertClassNameToVmFormat(sourceClass);
        }

        public String getSourceClass() {
            return sourceClass;
        }
    }

    public static class ExtendsAnnotatedClass extends AnnotationWithSourceClassUsage {
        private final String superClass;

        protected ExtendsAnnotatedClass(Set<String> annotations, String clazz, String superClass) {
            super(annotations, EXTENDS_CLASS, clazz);
            this.superClass = superClass;
        }

        public String getSuperClass() {
            return superClass;
        }
    }

    public static class ImplementsAnnotatedInterface extends AnnotationWithSourceClassUsage {
        private final String iface;
        public ImplementsAnnotatedInterface(Set<String> annotations, String clazz, String iface) {
            super(annotations, IMPLEMENTS_INTERFACE, clazz);
            this.iface = iface;
        }

        public String getInterface() {
            return iface;
        }
    }

    public static class AnnotatedFieldReference extends AnnotationWithSourceClassUsage {
        private final String fieldClass;
        private final String fieldName;

        public AnnotatedFieldReference(Set<String> annotations, String className, String fieldClass, String fieldName) {
            super(annotations, FIELD_REFERENCE, className);
            this.fieldClass = fieldClass;
            this.fieldName = fieldName;
        }

        public String getFieldClass() {
            return fieldClass;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    public static class AnnotatedMethodReference extends AnnotationWithSourceClassUsage {
        private final String methodClass;
        private final String methodName;
        private final String descriptor;

        public AnnotatedMethodReference(Set<String> annotations, String className, String methodClass, String methodName, String descriptor) {
            super(annotations, METHOD_REFERENCE, className);
            this.methodClass = methodClass;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        public String getMethodClass() {
            return methodClass;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getDescriptor() {
            return descriptor;
        }
    }

    public static class AnnotatedClassUsage extends AnnotationWithSourceClassUsage {
        private final String referencedClass;

        public AnnotatedClassUsage(Set<String> annotations, String className, String referencedClass) {
            super(annotations, CLASS_USAGE, className);
            this.referencedClass = referencedClass;
        }

        public String getReferencedClass() {
            return referencedClass;
        }
    }


    public static class AnnotatedAnnotation extends AnnotationUsage {
        public AnnotatedAnnotation(Set<String> annotations) {
            super(annotations, ANNOTATION_USAGE);
        }

    }

    public enum AnnotationUsageType {
        EXTENDS_CLASS,
        IMPLEMENTS_INTERFACE,
        METHOD_REFERENCE,
        FIELD_REFERENCE,
        CLASS_USAGE,
        ANNOTATION_USAGE;
    }


}
