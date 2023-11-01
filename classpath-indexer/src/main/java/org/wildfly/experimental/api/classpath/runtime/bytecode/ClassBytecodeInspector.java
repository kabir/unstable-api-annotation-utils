package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.wildfly.experimental.api.classpath.index.RuntimeIndex;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ConstantPool.AbstractRefInfo;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ConstantPool.NameAndTypeInfo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class ClassBytecodeInspector {

    private final ConstantPool pool;

    private ClassBytecodeInspector(ConstantPool pool) {
        this.pool = pool;
    }

    /**
     * Parses a class file
     *
     * @param classInputStream An input stream with the class bytes. A plain input stream may be used. This method will
     *                         wrap in a BufferedInputStream
     * @param runtimeIndex
     * @return a ClassBytecodeInspector instance for the class
     * @throws IOException
     */
    public static ClassBytecodeInspector parseClassFile(String className, InputStream classInputStream, RuntimeIndex runtimeIndex, BytecodeInspectionResultCollector resultCollector) throws IOException {
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
                            resultCollector.recordFieldUsage(annotations, className, declaringClass, refName);
                        }
                    } else {
                        String descriptor = constantPool.utf8(nameAndTypeInfo.descriptor_index);
                        Set<String> annotations = runtimeIndex.getAnnotationsForMethod(declaringClass, refName, descriptor);
                        if (annotations != null) {
                            resultCollector.recordMethodUsage(annotations, className, declaringClass, refName, descriptor);
                        }
                    }
                    break;
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
                resultCollector.recordSuperClassUsage(annotations, className, superClass);
            }
        }


        int interfacesCount = in.readUnsignedShort();
        for (int i = 0; i < interfacesCount; i++) {
            int interfaceIndex = in.readUnsignedShort();
            String iface = constantPool.className(interfaceIndex);
            Set<String> annotations = runtimeIndex.getAnnotationsForClass(iface);
            if (annotations != null) {
                resultCollector.recordImplementsInterfaceUsage(annotations, className, iface);
            }
        }

        return new ClassBytecodeInspector(constantPool);
    }


}
