package org.wildfly.unstable.api.annotation.classpath.index.benchmark;

import org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex.ByteArrayKey;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LookupMeasurements {

    private static String[] strA = {
            "org.wildfly.unstable.api.annotation.api",
            "org.wildfly.unstable.api.annotation.api",
            "org.wildfly.unstable.api.annotation.api.classpath",
            "org.wildfly.unstable.api.annotation.api.classpath.index",
            "org.wildfly.unstable.api.annotation.api.classpath.index.benchmark",
            "org.wildfly.test.unstable.api.annotation.api",
            "org.wildfly.test.unstable.api.annotation.api",
            "org.wildfly.test.unstable.api.annotation.api.classpath",
            "org.wildfly.test.unstable.api.annotation.api.classpath.index",
            "org.wildfly.test.unstable.api.annotation.api.classpath.index.benchmark",
            "org.wildfly.thing.unstable.api.annotation.api",
            "org.wildfly.thing.unstable.api.annotation.api",
            "org.wildfly.thing.unstable.api.annotation.api.classpath",
            "org.wildfly.thing.unstable.api.annotation.api.classpath.index",
            "org.wildfly.thing.unstable.api.annotation.api.classpath.index.benchmark",
            "org.wildfly.things.unstable.api.annotation.api",
            "org.wildfly.things.unstable.api.annotation.api",
            "org.wildfly.things.unstable.api.annotation.api.classpath",
            "org.wildfly.things.unstable.api.annotation.api.classpath.index",
            "org.wildfly.things.unstable.api.annotation.api.classpath.index.benchmark",
            "org.wildfly.time.unstable.api.annotation.api",
            "org.wildfly.time.unstable.api.annotation.api",
            "org.wildfly.time.unstable.api.annotation.api.classpath",
            "org.wildfly.time.unstable.api.annotation.api.classpath.index",
            "org.wildfly.time.unstable.api.annotation.api.classpath.index.benchmark",
            "org.wildfly.branch.unstable.api.annotation.api",
            "org.wildfly.branch.unstable.api.annotation.api",
            "org.wildfly.branch.unstable.api.annotation.api.classpath",
            "org.wildfly.branch.unstable.api.annotation.api.classpath.index",
            "org.wildfly.branch.unstable.api.annotation.api.classpath.index.benchmark",
            "org.wildfly.showcase.unstable.api.annotation.api",
            "org.wildfly.showcase.unstable.api.annotation.api",
            "org.wildfly.showcase.unstable.api.annotation.api.classpath",
            "org.wildfly.showcase.unstable.api.annotation.api.classpath.index",
            "org.wildfly.showcase.unstable.api.annotation.api.classpath.index.benchmark",
    };


    private static String[] strB = {
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN"
    };

    private static String[] strC = {
            "ein", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn",
            "Ein", "Zwei", "Drei", "Vier", "Fünf", "Sechs", "Sieben", "Acht", "Neun", "Zehn",
            "EIN", "ZWEI", "DREI", "VIER", "FÜNF", "SECHS", "SIEBEN", "ACHT", "NEUN", "ZEHN",
    };

    private static String[] strD = {
            "en", "to", "tre", "fire", "fem", "seks", "sju", "åtte", "ni", "ti",
            "En", "To", "Tre", "Fire", "Fem", "Seks", "Sju", "Åtte", "Ni", "Ti",
            "EN", "TO", "TRE", "FIRE", "FEM", "SEKS", "SJU", "ÅTTE", "NI", "TI",
    };

    private static String[] strE = {
            "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
            "Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Siete", "Ocho", "Nueve", "Diez",
            "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ"
    };


    List<String> strings = new ArrayList<>();
    public static void main(String[] args) throws Exception {

        LookupMeasurements test = new LookupMeasurements();
        test.createKeysBenchmark();
        test.lookupBenchmark();
    }

    private void createKeysBenchmark() throws Exception {
        List<byte[]> bytes = convertStringsToBytes(generateStrings());


        for (int i = 0; i < 5; i++) {
            List<ByteArrayKey> byteKeys = convertBytesToByteArrayKeys(bytes);
            List<String> strings = convertBytesToString(bytes);
        }
    }

    private void lookupBenchmark() throws IOException {
        List<String> strings = generateStrings();
        System.out.println(strings.size());
//        List<byte[]> bytes = convertStringsToBytes(strings);
//        System.out.println(bytes.size());
        List<ByteArrayKey> byteKeys = convertStringsToByteArrayKeys(strings);
        System.out.println(byteKeys.size());

        Map<String, String> stringMap10 = createMap(strings, 10);
        Map<String, String> stringMap100 = createMap(strings, 100);
        Map<ByteArrayKey, ByteArrayKey> byteArrayKeyMap10 = createMap(byteKeys, 10);
        Map<ByteArrayKey, ByteArrayKey> byteArrayKeyMap100 = createMap(byteKeys, 100);
        // Byte arrays don't actually work as keys
        //Map<byte[], byte[]> bytesMap10 = createMap(bytes, 10);
        //Map<byte[], byte[]> bytesMap100 = createMap(bytes, 100);

        for (int i = 0; i < 5; i++) {
            System.out.println("=====> " + i);
            searchMap("1/10 string map", strings, stringMap10);
            searchMap("1/100 string map", strings, stringMap100);
//            searchMap("1/10 byte map", bytes, bytesMap10);
//            searchMap("1/100 byte map", bytes, bytesMap100);
//            searchByteMapWithCopy("1/10 byte copied map", bytes, bytesMap10);
//            searchByteMapWithCopy("1/100 byte copied map", bytes, bytesMap100);
//            searchByteMapWithConversion("1/10 byte to string map", bytes, stringMap10);
//            searchByteMapWithConversion("1/100 byte to string map", bytes, stringMap100);
            searchMap("1/10 byte array key map", byteKeys, byteArrayKeyMap10);
            searchMap("1/100 byte array key map", byteKeys, byteArrayKeyMap100);
        }
    }

    //private void generateBytes

    private <V> void searchMap(String desc, List<V> list, Map<V, V> map) {
        System.gc();
        long start = System.currentTimeMillis();
        int count = 0;
        for (V v : list) {
            V val = map.get(v);
            if (val != null) {
                count++;
            }
        }
        long end = System.currentTimeMillis();

        System.out.println(desc + ": " + count + "took " + (end - start));
    }

    private void searchByteMapWithCopy(String desc, List<byte[]> list, Map<byte[], byte[]> map) {
        System.gc();
        long start = System.currentTimeMillis();
        int count = 0;
        for (byte[] v : list) {
            byte[] copy = Arrays.copyOfRange(v, 0, v.length);
            byte[] val = map.get(copy);
            if (val != null) {
                count++;
            }
        }
        long end = System.currentTimeMillis();

        System.out.println(desc + ": " + count + "took " + (end - start));
    }

    private void searchByteMapWithConversion(String desc, List<byte[]> list, Map<String, String> map) throws IOException {


            System.gc();
            long start = System.currentTimeMillis();
            int count = 0;
            for (byte[] v : list) {
                try (DataInputStream in = dataInputStreamOf(new ByteArrayInputStream(v))) {
                    String key = in.readUTF();
                    String val = map.get(key);
                    if (val != null) {
                        count++;
                    }
                }
            }
            long end = System.currentTimeMillis();

            System.out.println(desc + ": " + count + "took " + (end - start));

    }

    private <V> Map<V, V> createMap(List<V> strings, int modCount) {

        Map<V, V> map = new HashMap<>();
        int i = 0;
        for (V v : strings) {
            if (i % modCount == 0) {
                map.put(v, v);
            }
            i++;
        }
        return map;
    }

    private List<byte[]> convertStringsToBytes(List<String> strings) throws IOException {
        List<byte[]> bytes = new LinkedList<>();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (String s : strings) {
            try (DataOutputStream dout = new DataOutputStream(bout)) {
                bout.reset();
                dout.writeUTF(s);
                bytes.add(bout.toByteArray());
            }
        }
        return bytes;
    }

    private List<ByteArrayKey> convertStringsToByteArrayKeys(List<String> strings) throws IOException {
        List<ByteArrayKey> bytes = new LinkedList<>();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (String s : strings) {
            try (DataOutputStream dout = new DataOutputStream(bout)) {
                bout.reset();
                dout.writeUTF(s);
                byte[] arr = bout.toByteArray();
                bytes.add(ByteArrayKey.create(arr, 0, arr.length));
            }
        }
        return bytes;
    }

    private List<ByteArrayKey> convertBytesToByteArrayKeys(List<byte[]> bytes) {
        System.gc();
        long start = System.currentTimeMillis();
        List<ByteArrayKey> keys = new LinkedList<>();
        for (byte[] b : bytes) {
            keys.add(ByteArrayKey.create(b, 0, b.length));
        }
        long end = System.currentTimeMillis();
        System.out.println("Converting bytes to keys: " + (end -start));
        return keys;
    }

    private List<String> convertBytesToString(List<byte[]> bytes) throws IOException {
        System.gc();
        long start = System.currentTimeMillis();
        List<String> strings = new LinkedList<>();
        ReusableBufferedDataInputStream in = new ReusableBufferedDataInputStream();
        ReusableBufferedInputStream bin = new ReusableBufferedInputStream();
        in.setInputStream(bin);
        for (byte[] b : bytes) {
            bin.setInputStream(new ByteArrayInputStream(b));
            strings.add(in.readUTF());
            bin.close();
        }
        long end = System.currentTimeMillis();
        System.out.println("Converting bytes to strings : " + (end -start));
        return strings;
    }

    private List<String> generateStrings() {
        List<String> result = new LinkedList<>();
        result.addAll(generateStrings(strA, strB, strC, strD, strE));
        return result;
    }

    private List<String> generateStrings(String[] arr1, String[] arr2, String[] arr3, String[] arr4, String[] arr5) {
        List<String> result = new ArrayList<>();
        for (String s1 : arr1) {
            for (String s2 : arr2) {
                for (String s3 : arr3) {
                    for (String s4 : arr4) {
                        for (String s5 : arr5) {
                            result.add(s1 + "." + s2 + "." + s3 + "." + s4 + "." + s5);
                        }
                    }
                }
            }
        }
        return result;
    }


    private class ResettableByteInputStream extends ByteArrayInputStream {
        ResettableByteInputStream() {
            super(new byte[1024]);
        }
        public void setBytes(byte[] bytes) {
            buf = bytes;
            pos = 0;
        }
    }


    private ReusableBufferedDataInputStream dataInputStream;

    DataInputStream dataInputStreamOf(InputStream inputStream) {
        ReusableBufferedDataInputStream stream = dataInputStream;
        if (stream == null) {
            stream = new ReusableBufferedDataInputStream();
            this.dataInputStream = stream;
        }
        stream.setInputStream(inputStream);
        return stream;
    }


    static final class ReusableBufferedDataInputStream extends DataInputStream {
        private ReusableBufferedInputStream reusableBuffered = null;

        ReusableBufferedDataInputStream() {
            super(null);
        }

        void setInputStream(InputStream in) {
            Objects.requireNonNull(in);
            // this is already buffered: let's use it directly
            if (in instanceof BufferedInputStream) {
                assert !(in instanceof ReusableBufferedInputStream);
                this.in = in;
            } else {
                if (this.in == null) {
                    if (reusableBuffered == null) {
                        reusableBuffered = new ReusableBufferedInputStream();
                    }
                    this.in = reusableBuffered;
                }
                reusableBuffered.setInputStream(in);
            }
        }

        @Override
        public void close() {
            if (in == reusableBuffered) {
                reusableBuffered.close();
            } else {
                in = null;
            }
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        @Deprecated
        public synchronized void mark(int readlimit) {
            throw new UnsupportedOperationException("mark/reset not supported");
        }

        @Override
        @Deprecated
        public synchronized void reset() {
            throw new UnsupportedOperationException("mark/reset not supported");
        }
    }

    private static final class ReusableBufferedInputStream extends BufferedInputStream {
        private ReusableBufferedInputStream() {
            super(null);
        }

        void setInputStream(InputStream in) {
            Objects.requireNonNull(in);
            if (pos != 0 && this.in != null) {
                throw new IllegalStateException("the stream cannot be reused");
            }
            this.in = in;
        }

        @Override
        public void close() {
            in = null;
            count = 0;
            pos = 0;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        @Deprecated
        public synchronized void mark(int readlimit) {
            throw new UnsupportedOperationException("mark/reset not supported");
        }

        @Override
        @Deprecated
        public synchronized void reset() {
            throw new UnsupportedOperationException("mark/reset not supported");
        }
    }


}
