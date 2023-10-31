package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.wildfly.experimental.api.classpath.index.RuntimeIndex;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

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
    public static ClassBytecodeInspector parseClassFile(InputStream classInputStream, RuntimeIndex runtimeIndex) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(classInputStream));

        // Parse the stuff before the ConstantPool
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("Not a valid class file (no CAFEBABE header)");
        }
        int minor_version = in.readUnsignedShort();
        int major_version = in.readUnsignedShort();


        ConstantPool pool = ConstantPool.read(in);



        // TODO read the superclass and interfaces
//        int access_flags = in.readUnsignedShort();
//
//        int this_class_index = in.readUnsignedShort();
//        String this_class = constant_pool.className(this_class_index);
//
//        int super_class_index = in.readUnsignedShort();
//        String super_class = (super_class_index != 0) ? constant_pool.className(super_class_index) : null;
//
//        int interfaces_count = in.readUnsignedShort();
//        String[] interfaces = new String[interfaces_count];
//        for (int i = 0; i < interfaces_count; i++) {
//            int interface_index = in.readUnsignedShort();
//            interfaces[i] = constant_pool.className(interface_index);
//        }



        return new ClassBytecodeInspector(pool);
    }

}
