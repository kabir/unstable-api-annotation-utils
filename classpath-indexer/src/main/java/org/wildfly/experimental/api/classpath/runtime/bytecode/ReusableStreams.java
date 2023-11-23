package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class ReusableStreams {
    private final ReusableByteArrayDataInputStream dataInput = new ReusableByteArrayDataInputStream(new ReplaceableByteArrayInputStream(new byte[0], 0, 0));

    public DataInputStream getDataInputStream(byte[] buf, int offset, int length) {
        dataInput.setByteInput(buf, offset, length);
        return dataInput;
    }

    static class ReusableByteArrayDataInputStream extends DataInputStream {
        private final ReplaceableByteArrayInputStream byteInput;

        private ReusableByteArrayDataInputStream(ReplaceableByteArrayInputStream in) {
            super(in);
            byteInput = in;
        }

        public void setByteInput(byte[] buf, int offset, int length) {
            byteInput.setByteInput(buf, offset, length);
        }

        @Override
        public void close() throws IOException {
            // We don't want to close anything, but we'll clear the array from the bytearrayinputstream
            byteInput.buf = null;
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    }

    private static class ReplaceableByteArrayInputStream extends InputStream {

        protected byte buf[];

        protected int pos;


        protected int count;

        public ReplaceableByteArrayInputStream(byte buf[], int offset, int length) {
            this.buf = buf;
            this.pos = offset;
            this.count = Math.min(offset + length, buf.length);
        }

        void setByteInput(byte[] buf, int offset, int length) {
            this.buf = buf;
            this.pos = offset;
            this.count = Math.min(offset + length, buf.length);
        }

        public synchronized int read() {
            return (pos < count) ? (buf[pos++] & 0xff) : -1;
        }

        public synchronized int read(byte b[], int off, int len) {
            Objects.checkFromIndexSize(off, len, b.length);

            if (pos >= count) {
                return -1;
            }

            int avail = count - pos;
            if (len > avail) {
                len = avail;
            }
            if (len <= 0) {
                return 0;
            }
            System.arraycopy(buf, pos, b, off, len);
            pos += len;
            return len;
        }

        public synchronized byte[] readAllBytes() {
            byte[] result = Arrays.copyOfRange(buf, pos, count);
            pos = count;
            return result;
        }

        public int readNBytes(byte[] b, int off, int len) {
            int n = read(b, off, len);
            return n == -1 ? 0 : n;
        }

        public synchronized long transferTo(OutputStream out) throws IOException {
            int len = count - pos;
            out.write(buf, pos, len);
            pos = count;
            return len;
        }

        public synchronized long skip(long n) {
            long k = count - pos;
            if (n < k) {
                k = n < 0 ? 0 : n;
            }

            pos += k;
            return k;
        }

        public synchronized int available() {
            return count - pos;
        }

        public boolean markSupported() {
            return false;
        }

        public void mark(int readAheadLimit) {
            // This should not happen since we don't support mark
            throw new IllegalStateException();
        }

        public synchronized void reset() {
            pos = 0;
        }

        public void close() throws IOException {
        }

    }
}
