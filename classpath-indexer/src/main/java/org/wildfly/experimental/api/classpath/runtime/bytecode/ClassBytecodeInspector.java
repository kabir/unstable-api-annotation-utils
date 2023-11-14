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
import java.util.Objects;
import java.util.Set;

import static org.wildfly.experimental.api.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
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

    public static abstract class AnnotationUsage {
        protected final Set<String> annotations;
        protected final AnnotationUsageType type;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnnotationUsage usage = (AnnotationUsage) o;
            return Objects.equals(annotations, usage.annotations) && type == usage.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(annotations, type);
        }

        // When reading the bytecode, the class names will be in JVM format (e.g. java/lang/Class).
        // We keep that for fast lookups during the indexing.
        // When returning the data to the users we convert to the more familiar 'dot format' (i.e. java.lang.Class)
        protected abstract AnnotationUsage convertToDotFormat();
    }

    public static abstract class AnnotationWithSourceClassUsage extends AnnotationUsage {
        protected final String sourceClass;

        private AnnotationWithSourceClassUsage(Set<String> annotations, AnnotationUsageType type, String sourceClass) {
            super(annotations, type);
            this.sourceClass = sourceClass;
        }

        public String getSourceClass() {
            return sourceClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AnnotationWithSourceClassUsage that = (AnnotationWithSourceClassUsage) o;
            return Objects.equals(sourceClass, that.sourceClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), sourceClass);
        }
    }

    public static class ExtendsAnnotatedClass extends AnnotationWithSourceClassUsage {
        private final String superClass;

        private ExtendsAnnotatedClass(Set<String> annotations, String clazz, String superClass) {
            super(annotations, EXTENDS_CLASS, clazz);
            this.superClass = superClass;
        }

        public String getSuperClass() {
            return superClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ExtendsAnnotatedClass that = (ExtendsAnnotatedClass) o;
            return Objects.equals(superClass, that.superClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), superClass);
        }

        @Override
        protected AnnotationUsage convertToDotFormat() {
            return new ExtendsAnnotatedClass(
                    annotations,
                    convertClassNameToDotFormat(sourceClass),
                    convertClassNameToDotFormat(superClass));
        }
    }

    public static class ImplementsAnnotatedInterface extends AnnotationWithSourceClassUsage {
        private final String iface;
        private ImplementsAnnotatedInterface(Set<String> annotations, String clazz, String iface) {
            super(annotations, IMPLEMENTS_INTERFACE, clazz);
            this.iface = iface;
        }

        public String getInterface() {
            return iface;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ImplementsAnnotatedInterface that = (ImplementsAnnotatedInterface) o;
            return Objects.equals(iface, that.iface);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), iface);
        }

        @Override
        protected AnnotationUsage convertToDotFormat() {
            return new ImplementsAnnotatedInterface(
                    annotations,
                    convertClassNameToDotFormat(sourceClass),
                    convertClassNameToDotFormat(iface));
        }
    }

    public static class AnnotatedFieldReference extends AnnotationWithSourceClassUsage {
        private final String fieldClass;
        private final String fieldName;

        private AnnotatedFieldReference(Set<String> annotations, String className, String fieldClass, String fieldName) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AnnotatedFieldReference that = (AnnotatedFieldReference) o;
            return Objects.equals(fieldClass, that.fieldClass) && Objects.equals(fieldName, that.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fieldClass, fieldName);
        }

        @Override
        protected AnnotationUsage convertToDotFormat() {
            return new AnnotatedFieldReference(
                    annotations,
                    convertClassNameToDotFormat(sourceClass),
                    convertClassNameToDotFormat(fieldClass), fieldName);
        }
    }

    public static class AnnotatedMethodReference extends AnnotationWithSourceClassUsage {
        private final String methodClass;
        private final String methodName;
        private final String descriptor;

        private AnnotatedMethodReference(Set<String> annotations, String className, String methodClass, String methodName, String descriptor) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AnnotatedMethodReference that = (AnnotatedMethodReference) o;
            return Objects.equals(methodClass, that.methodClass) && Objects.equals(methodName, that.methodName) && Objects.equals(descriptor, that.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), methodClass, methodName, descriptor);
        }

        @Override
        protected AnnotationUsage convertToDotFormat() {
            return new AnnotatedMethodReference(
                    annotations,
                    convertClassNameToDotFormat(sourceClass),
                    convertClassNameToDotFormat(methodClass),
                    methodName,
                    descriptor);
        }
    }

    public static class AnnotatedClassUsage extends AnnotationWithSourceClassUsage {
        private final String referencedClass;

        private AnnotatedClassUsage(Set<String> annotations, String className, String referencedClass) {
            super(annotations, CLASS_USAGE, className);
            this.referencedClass = referencedClass;
        }

        public String getReferencedClass() {
            return referencedClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AnnotatedClassUsage usage = (AnnotatedClassUsage) o;
            return Objects.equals(referencedClass, usage.referencedClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), referencedClass);
        }

        @Override
        protected AnnotationUsage convertToDotFormat() {
            return new AnnotatedClassUsage(
                    annotations,
                    convertClassNameToDotFormat(sourceClass),
                    convertClassNameToDotFormat(referencedClass));
        }
    }


    public static class AnnotatedAnnotation extends AnnotationUsage {
        private AnnotatedAnnotation(Set<String> annotations) {
            super(annotations, ANNOTATION_USAGE);
        }

        @Override
        protected AnnotationUsage convertToDotFormat() {
            return new AnnotatedAnnotation(annotations);
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
