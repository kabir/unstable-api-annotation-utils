package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.ByteArrayKey;

import java.io.IOException;

import static org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex.convertClassNameToDotFormat;

public class ClassInformation {

    private static final ByteArrayKey[] EMPTY_BYTE_ARRAY_KEY_ARRAY = new ByteArrayKey[0];

    private final int[] tags;
    private final byte[] constPoolBytes;
    private final int[] offsets;
    private final int thisClassPosition;
    private final int superClassPosition;
    private final int[] interfacePositions;

    private final ByteArrayKey[] byteArrayKeys;
    private final int constantPoolSize;

    private ByteArrayKey scannedClassKey;
    private String scannedClassName;


    ClassInformation(int[] tags, byte[] constPoolBytes, int[] offsets, int thisClassPosition, int superClassPosition, int[] interfacePositions, int constantPoolSize) {
        this.tags = tags;
        this.constPoolBytes = constPoolBytes;
        this.offsets = offsets;
        this.thisClassPosition = thisClassPosition;
        this.superClassPosition = superClassPosition;
        this.interfacePositions = interfacePositions;
        this.byteArrayKeys = new ByteArrayKey[tags.length];
        this.constantPoolSize = constantPoolSize;
    }

    ByteArrayKey getClassNameFromRefInfo(int constantPoolPosition) {
        int index = constantPoolPosition - 1;
        int classPosition = readUnsignedShortByConstantPoolOffset(offsets[index]);
        return getClassNameFromClassInfo(classPosition);
    }

    ByteArrayKey getNameFromRefInfo(int constantPoolPosition) {
        int nameAndTypeInfoPosition = getNameAndTypeInfoPositionFromRefInfo(constantPoolPosition);
        int offset = offsets[nameAndTypeInfoPosition -1];
        // The name will be the first two bytes of the constantPool entry
        int refNamePosition = readUnsignedShortByConstantPoolOffset(offset);
        return getKeyFromUtfInfo(refNamePosition);
    }

    ByteArrayKey getDescriptorFromRefInfo(int constantPoolPosition) {
        int nameAndTypeInfoPosition = getNameAndTypeInfoPositionFromRefInfo(constantPoolPosition);
        int offset = offsets[nameAndTypeInfoPosition -1];
        // The name will be the second two bytes of the constantPool entry (first two contain the name)
        int refNamePosition = readUnsignedShortByConstantPoolOffset(offset + 2);
        return getKeyFromUtfInfo(refNamePosition);
    }

    ByteArrayKey getClassNameFromClassInfo(int constantPoolPosition) {
        int index = constantPoolPosition - 1;
        ByteArrayKey key = byteArrayKeys[index];
        if (key == null) {
            // ClassInfo just contains the location of the UtfInfo containing the class bane
            int utfInfoPosition = readUnsignedShortByConstantPoolOffset(offsets[index]);
            key = getKeyFromUtfInfo(utfInfoPosition);
            byteArrayKeys[index] = key;
        }
        return key;
    }

    ByteArrayKey getScannedClass() {
        if (scannedClassKey == null) {
            scannedClassKey = getClassNameFromClassInfo(thisClassPosition);
        }
        return scannedClassKey;
    }

    String getScannedClassName(ReusableStreams reusableStreams) throws IOException {
        if (scannedClassName == null) {
            ByteArrayKey key = getClassNameFromClassInfo(thisClassPosition);
            scannedClassName = convertClassNameToDotFormat(key.convertBytesToString(reusableStreams));
        }
        return scannedClassName;
    }

    ByteArrayKey getSuperClass() {
        if (superClassPosition == 0) {
            return null;
        }
        return getClassNameFromClassInfo(superClassPosition);
    }

    ByteArrayKey[] getInterfaces() {
        if (interfacePositions.length == 0) {
            return EMPTY_BYTE_ARRAY_KEY_ARRAY;
        }
        ByteArrayKey[] keys = new ByteArrayKey[interfacePositions.length];
        for (int i = 0; i < interfacePositions.length; i++) {
            keys[i] = getClassNameFromClassInfo(interfacePositions[i]);
        }
        return keys;
    }

    private int getNameAndTypeInfoPositionFromRefInfo(int constantPoolPosition) {
        int index = constantPoolPosition - 1;
        // For a Field-/Method-/InterfaceMethodRefInfo, the location of the
        // NameAndTypeInfo will be the bytes after the first two (those contain the
        // position of the classInfo)
        int refInfoOffset = offsets[index];
        int position = readUnsignedShortByConstantPoolOffset(refInfoOffset + 2);
        return position;
    }


    private ByteRuntimeIndex.ByteArrayKey getKeyFromUtfInfo(int constantPoolPosition) {
        int index = constantPoolPosition - 1;
        ByteRuntimeIndex.ByteArrayKey key = byteArrayKeys[index];
        if (key == null) {
            int offset = offsets[index];

            // The length of the constant pool entry is the difference between the next offset
            // and the current offset.
            // If we are at the last offset, the 'next' offset is the actual length of the constant pool.
            // If there had been another entry, that would have been where it would have gone.
            int nextOffset =
                    index == (offsets.length - 1) ?
                            constantPoolSize : offsets[index + 1];
            int length = nextOffset - offset;


            key = ByteArrayKey.create(constPoolBytes, offset, length);
            byteArrayKeys[index] = key;
        }
        return key;
    }

    private int readUnsignedShortByConstantPoolOffset(int offset) {
        // Stolen from DataInputStream
        int ch1 = readByteAsUnsignedIntByConstantPoolOffset(offset);
        int ch2 = readByteAsUnsignedIntByConstantPoolOffset(offset + 1);
        if ((ch1 | ch2) < 0)
            throw new IllegalStateException();
        return (ch1 << 8) + ch2;
    }

    private int readByteAsUnsignedIntByConstantPoolOffset(int offset) {
        // Reads a byte as an unsigned int, the same way ByteArrayInputStream does
        return constPoolBytes[offset] & 0xff;
    }

    public int[] getTags() {
        return tags;
    }
}
