package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public class FastClassInfoScanner {

    // Constant Pool constants


    private final FastInfoCollector collector;
    private final TmpObjects tmpObjects = new TmpObjects();

    public FastClassInfoScanner(ByteRuntimeIndex runtimeIndex) {
        this.collector = new FastInfoCollector(runtimeIndex);
    }

    public Set<AnnotationUsage> getUsages() {
        return collector.getUsages();
    }

    public void scanClass(InputStream input) throws IOException {
        BufferedInputStream in = input instanceof BufferedInputStream ?
                (BufferedInputStream) input : new BufferedInputStream(input);
        verifyMagic(in);
        boolean checkJava11AndNewer = true; // Toggle this for the standalone benchmark
        if (!readVersionFields(in, checkJava11AndNewer)) {
            return;
        }

        int size = readUnsignedShort(in) - 1;

        byte[] constPool = null;
        int[] offsets = null;
        int[] tags = null;
        try {
            constPool = tmpObjects.borrowConstantPool(size);
            offsets = new int[size];
            tags = new int[size];
            int lastOffset = 0;
            for (int pos = 0, offset = 0; pos < size; pos++) {
                int tag = readUnsignedByte(in);
                offsets[pos] = offset;
                tags[pos] = tag;
                switch (tag) {
                    case BytecodeTags.CONSTANT_CLASS:
                    case BytecodeTags.CONSTANT_STRING:
                    case BytecodeTags.CONSTANT_METHODTYPE:
                    case BytecodeTags.CONSTANT_MODULE:
                    case BytecodeTags.CONSTANT_PACKAGE:
                        constPool = sizeToFit(constPool, 2, offset, size - pos);
                        tags[pos] = tag;
                        readFully(in, constPool, offset, 2);
                        offset += 2;
                        break;
                    case BytecodeTags.CONSTANT_FIELDREF:
                    case BytecodeTags.CONSTANT_METHODREF:
                    case BytecodeTags.CONSTANT_INTERFACEMETHODREF:
                    case BytecodeTags.CONSTANT_INTEGER:
                    case BytecodeTags.CONSTANT_INVOKEDYNAMIC:
                    case BytecodeTags.CONSTANT_DYNAMIC:
                    case BytecodeTags.CONSTANT_FLOAT:
                    case BytecodeTags.CONSTANT_NAMEANDTYPE:
                        constPool = sizeToFit(constPool, 4, offset, size - pos);
                        tags[pos] = tag;
                        readFully(in, constPool, offset, 4);
                        offset += 4;
                        break;
                    case BytecodeTags.CONSTANT_LONG:
                    case BytecodeTags.CONSTANT_DOUBLE:
                        constPool = sizeToFit(constPool, 8, offset, size - pos);
                        tags[pos] = tag;
                        readFully(in, constPool, offset, 8);
                        offset += 8;
                        pos++; // 8 byte constant pool entries take two "virtual" slots for some reason
                        break;
                    case BytecodeTags.CONSTANT_METHODHANDLE:
                        constPool = sizeToFit(constPool, 3, offset, size - pos);
                        tags[pos] = (byte) tag;
                        readFully(in, constPool, offset, 3);
                        offset += 3;
                        break;
                    case BytecodeTags.CONSTANT_UTF8:
                        int len = readUnsignedShort(in);
                        constPool = sizeToFit(constPool, len + 2, offset, size - pos);
                        tags[pos] = tag;
                        constPool[offset++] = (byte) (len >>> 8);
                        constPool[offset++] = (byte) len;

                        readFully(in, constPool, offset, len);
                        offset += len;
                        break;
                    default:
                        throw new IllegalStateException(
                                String.format(Locale.ROOT, "Unknown tag %s! pos = %s poolSize = %s", tag, pos, size));
                }
                lastOffset = offset;
            }

            // Skip the access flags
            skipBytes(in, 2);

            int thisClassPosition = readUnsignedShort(in);
            int superClassPosition = readUnsignedShort(in);
            int interfacesCount = readUnsignedShort(in);
            int[] interfacePositions = new int[interfacesCount];
            for (int i = 0; i < interfacesCount; i++) {
                interfacePositions[i] = readUnsignedShort(in);
            }

            FastClassInformation classInfo =
                    new FastClassInformation(tags, constPool, offsets, thisClassPosition, superClassPosition, interfacePositions, lastOffset);
            collector.processClass(classInfo);

        } finally {
            if (constPool != null) {
                tmpObjects.returnConstantPool(constPool);
            }
        }
    }

    private void verifyMagic(InputStream in) throws IOException {
        final int magic;
        try {
            magic = readInteger(in);
        } catch (EOFException e) {
            throw new EOFException("Input is not a valid class file; must begin with a 4-byte integer 0xCAFEBABE");
        }
        if (magic != 0xCA_FE_BA_BE) {
            throw new IOException("Input is not a valid class file; must begin with a 4-byte integer 0xCAFEBABE, "
                    + "but seen 0x" + Integer.toHexString(magic).toUpperCase());
        }
    }
    private boolean readVersionFields(InputStream in, boolean checkJava11AndNewer) throws IOException {
        int minor = readUnsignedShort(in);
        int major = readUnsignedShort(in);
        if (checkJava11AndNewer) {
            return major > 45 || (major == 45 && minor >= 3);
        }
        return true;
    }

    private int readInteger(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    private int readUnsignedByte(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    private int readUnsignedShort(InputStream in) throws IOException {
        // Stolen from DataInputStream
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new IllegalStateException();
        return (ch1 << 8) + ch2;
    }

    private byte[] sizeToFit(byte[] buf, int needed, int offset, int remainingEntries) {
        int oldLength = buf.length;
        if (offset + needed > oldLength) {
            int newLength = newLength(oldLength, needed, oldLength >> 1);
            buf = Arrays.copyOf(buf, newLength);
        }
        return buf;
    }

    private int newLength(int oldLength, int minGrowth, int prefGrowth) {
        int prefLength = oldLength + Math.max(minGrowth, prefGrowth);
        return prefLength > 0 ? prefLength : minLength(oldLength, minGrowth);
    }

    private int minLength(int oldLength, int minGrowth) {
        int minLength = oldLength + minGrowth;
        if (minLength < 0) {
            throw new OutOfMemoryError("Cannot allocate a large enough array: " +
                    oldLength + " + " + minGrowth + " is too large");
        }
        return minLength;
    }

    private void readFully(InputStream in, byte[] buf, int offset, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(buf, offset + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

    private int skipBytes(InputStream in, int n) throws IOException {
        int total = 0;
        int cur = 0;

        while ((total<n) && ((cur = (int) in.skip(n-total)) > 0)) {
            total += cur;
        }

        return total;
    }

    private static final class TmpObjects {
        //private Utils.ReusableBufferedDataInputStream dataInputStream;

        private byte[] constantPool;

        byte[] borrowConstantPool(int poolSize) {
            byte[] buf = this.constantPool;
            if (buf == null || buf.length < (20 * poolSize)) {
                buf = new byte[20 * poolSize]; // Guess
            } else {
                Arrays.fill(buf, 0, poolSize, (byte) 0);
            }
            this.constantPool = null;
            return buf;
        }

        void returnConstantPool(byte[] buf) {
            this.constantPool = buf;
        }
    }

}
