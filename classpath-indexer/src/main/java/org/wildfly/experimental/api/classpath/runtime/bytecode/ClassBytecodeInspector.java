package org.wildfly.experimental.api.classpath.runtime.bytecode;

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
     * @param classInputStream An input stream with the class bytes. A plain input stream may be used. This method will
     *                         wrap in a BufferedInputStream
     * @return a ClassBytecodeInspector instance for the class
     * @throws IOException
     */
    public static ClassBytecodeInspector parseClassFile(InputStream classInputStream) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(classInputStream));

        // Parse the stuff before the ConstantPool
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("Not a valid class file (no CAFEBABE header)");
        }
        int minor_version = in.readUnsignedShort();
        int major_version = in.readUnsignedShort();


        ConstantPool pool = ConstantPool.read(in);
        return new ClassBytecodeInspector(pool);
    }

}
