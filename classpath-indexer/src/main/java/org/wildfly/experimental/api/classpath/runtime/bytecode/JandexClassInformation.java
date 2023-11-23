package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

class JandexClassInformation {
    private final int[] tags;
    private final int[] offsets;
    private final int[] lengths;

    // Cache the ByteArrayKeys read at the corresponding ClassInfo entry locations
    private final ByteRuntimeIndex.ByteArrayKey[] byteArrayKeys;

    private byte[] constantPool;

    private DotName thisName;
    private String thisNameStr;
    private Type superClassType;
    private Type[] interfaceTypes;


    JandexClassInformation(int constantPoolSize) {
        tags = new int[constantPoolSize + 1];
        offsets = new int[constantPoolSize + 1];
        lengths = new int[constantPoolSize + 1];
        byteArrayKeys = new ByteRuntimeIndex.ByteArrayKey[constantPoolSize + 1];
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
     *
     * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
     * @return the name of the class containing the reference
     */
    ByteRuntimeIndex.ByteArrayKey getClassNameFromReference(int constantPoolPosition) {
        // The location of the ClassInfo will be the first two bytes
        int classPosition = readUnsignedShort(offsets[constantPoolPosition]);
        // Get the name of the class
        return getClassNameFromClassInfo(classPosition);
    }

    /**
     * FieldRefInfo, MethodRedInfo and InterfaceRefInfo all have the same structure.
     * This method finds the NameAndTypeInfo from the XXX RedInfo. In the NameAndTypeInfo
     * the index of the UTFInfo containing the name will be stored in the first two bytes.
     *
     * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
     * @return the name of the field/method pointed at by the XXXRefInfo
     */
    ByteRuntimeIndex.ByteArrayKey getNameFromReference(int constantPoolPosition) {
        int nameAndTypeInfoOffset = getNameAndTypeInfoOffsetFromReference(constantPoolPosition);
        // The location of the NameAndTypeInfo will be its fist two bytes
        int refNamePosition = readUnsignedShort(nameAndTypeInfoOffset);
        return getKeyFromUtfInfo(refNamePosition);
    }

    /**
     * FieldRefInfo, MethodRedInfo and InterfaceRefInfo all have the same structure.
     * This method finds the NameAndTypeInfo from the XXX RedInfo. In the NameAndTypeInfo
     * the index of the UTFInfo containing the descriptor will be stored in the third and fourth bytes.
     *
     * @param constantPoolPosition the position of the XXXRefInfo entry in the constant pool
     * @return the name of the field/method pointed at by the XXXRefInfo
     */
    ByteRuntimeIndex.ByteArrayKey getDescriptorFromReference(int constantPoolPosition) {
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

    ByteRuntimeIndex.ByteArrayKey getClassNameFromClassInfo(int constantPoolPosition) {
        ByteRuntimeIndex.ByteArrayKey key = byteArrayKeys[constantPoolPosition];
        if (key == null) {
            // ClassInfo just contains the location of the UtfInfo containing the class bane
            int utfInfoPosition = readUnsignedShort(offsets[constantPoolPosition]);
            key = getKeyFromUtfInfo(utfInfoPosition);
            byteArrayKeys[constantPoolPosition] = key;
        }
        return key;
    }

    private ByteRuntimeIndex.ByteArrayKey getKeyFromUtfInfo(int constantPoolPosition) {
        ByteRuntimeIndex.ByteArrayKey key = byteArrayKeys[constantPoolPosition];
        if (key == null) {
            key = ByteRuntimeIndex.ByteArrayKey.create(constantPool, offsets[constantPoolPosition], lengths[constantPoolPosition]);
            byteArrayKeys[constantPoolPosition] = key;
        }
        return key;
    }

    // Indexes in the array are stored as two bytes, and interpreted as unsigned shorts
    // This replicates how DataInputStream reads unsigned shorts
    private int readUnsignedShort(int offset) {
        // Stolen from DataInputStream
        int ch1 = readByteAsUnsignedInt(offset);
        int ch2 = readByteAsUnsignedInt(offset + 1);
        if ((ch1 | ch2) < 0)
            throw new IllegalStateException();
        return (ch1 << 8) + ch2;
    }

    private int readByteAsUnsignedInt(int offset) {
        // Reads a byte as an unsigned int, the same way ByteArrayInputStream does
        return constantPool[offset] & 0xff;
    }

    String getScannedClassName() {
        if (thisNameStr == null) {
            thisNameStr = thisName.toString();
        }
        return thisNameStr;
    }

    public int[] getTags() {
        return tags;
    }

    public int[] getOffsets() {
        return offsets;
    }

    public int[] getLengths() {
        return lengths;
    }

    public ByteRuntimeIndex.ByteArrayKey[] getByteArrayKeys() {
        return byteArrayKeys;
    }

    public byte[] getConstantPool() {
        return constantPool;
    }

    public void setConstantPool(byte[] constantPool) {
        this.constantPool = constantPool;
    }

    public DotName getThisName() {
        return thisName;
    }

    public void setThisName(DotName thisName) {
        this.thisName = thisName;
    }

    public String getThisNameStr() {
        return thisNameStr;
    }

    public void setThisNameStr(String thisNameStr) {
        this.thisNameStr = thisNameStr;
    }

    public Type getSuperClassType() {
        return superClassType;
    }

    public void setSuperClassType(Type superClassType) {
        this.superClassType = superClassType;
    }

    public Type[] getInterfaceTypes() {
        return interfaceTypes;
    }

    public void setInterfaceTypes(Type[] interfaceTypes) {
        this.interfaceTypes = interfaceTypes;
    }

    DebugUtils getDebugUtils() {
        return new DebugUtils();
    }
    class DebugUtils {
        public void outputRawFormat() {
            for (int pos = 1; pos < tags.length; pos++) {
                int tag = tags[pos];
                String tagName = formatTag(tag);
                int offset = offsets[pos];
                int length = lengths[pos];
                byte[] data = Arrays.copyOfRange(constantPool, offset, offset + length);
                System.out.printf("%d\t - %s (%d,%d)\t\t%s\n", pos, tagName, offset, length, Arrays.toString(data));
                dumpArrayToFile(pos, data);
                //System.out.printf("\t%s\n", Arrays.toString(data));
            }
        }

        private void dumpArrayToFile(int pos, byte[] data) {
            try {
                Path path = Paths.get("target/arrays/" + thisName.toString());
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
                Path file = path.resolve(pos + ".txt");
                if (Files.exists(file)) {
                    Files.delete(file);
                }
                Files.createFile(file);
                try (FileOutputStream fout = new FileOutputStream(file.toFile())) {
                    fout.write(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String formatTag(int tag) {
            switch (tag) {
                case 1:
                    return "UTF_8(1)  ";
                case 3:
                    return "Integ(3)  ";
                case 4:
                    return "Float(4)  ";
                case 5:
                    return "Long(5)   ";
                case 6:
                    return "Doubl(6)  ";
                case 7:
                    return "Class(7)  ";
                case 8:
                    return "Str(8)    ";
                case 9:
                    return "Fld(9)    ";
                case 10:
                    return "Mtd(10)   ";
                case 11:
                    return "IMtd(11)  ";
                case 12:
                    return "NmTy(12)  ";
                case 15:
                    return "MHad(15)  ";
                case 17:
                    return "Dyn(17)   ";
                case 18:
                    return "InvDy(18) ";
                case 19:
                    return "Modul(19) ";
                case 20:
                    return "Pkg(20)   ";
                default:
                    return "???????   ";
            }
        }
    }

}
