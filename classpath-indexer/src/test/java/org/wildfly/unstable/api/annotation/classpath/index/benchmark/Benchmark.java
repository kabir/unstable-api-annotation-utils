package org.wildfly.unstable.api.annotation.classpath.index.benchmark;

import org.jboss.jandex.Indexer;
import org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex;
import org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.ClassInfoScanner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Util to run to get a sense of how long it takes to scan on a lot of classes.
 *
 * Parameters:
 *
 * 1) Path to a file containing the classpath to scan (e.g. as output by mvn dependency:build-classpath -Dmdep.outputFile=cp.txt)
 * 2) Path to a directory containing an index.txt with the names of the serialized OutputIndex files i.e as bundled in WildFly)
 */
public class Benchmark {

    public static void main(String[] args) throws Exception {
        Path classpathFile = Paths.get(args[0]);
        Path indexDir = Paths.get(args[1]);

        String classpathString = Files.readString(classpathFile);
        List<Path> classpath = new ArrayList<>();
        for (String s : classpathString.split(":")) {
            if (!s.endsWith(".jar")) {
                continue;
            }
            classpath.add(Paths.get(s));
        }

        Path indexFile = indexDir.resolve("index.txt");
        RuntimeIndex runtimeIndex;
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile.toFile()))) {
            List<URL> list = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                list.add(indexDir.resolve(line).toUri().toURL());
                line = reader.readLine();
            }
            runtimeIndex = RuntimeIndex.load(list);
        }

        Map<String, List<Long>> runningTimes = new LinkedHashMap<>();

        final int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            System.out.println("==== Iteration " + i);
            new JarReader(runningTimes, classpath, new NullWorker()).indexJar();
            new JarReader(runningTimes, classpath, new ConsumeAllBytesWorker()).indexJar();
            new JarReader(runningTimes, classpath, new JandexWorker()).indexJar();
            new JarReader(runningTimes, classpath, new FastScannerWorker(runtimeIndex)).indexJar();
        }

        System.out.println("==== Final Results for " + iterations + " iterations");
        for (String type : runningTimes.keySet()) {
            long sum = 0;
            for (long l : runningTimes.get(type)) {
                sum += l;
            }
            long average = sum / iterations;
            System.out.println("\t*" + type + " - Average: " + average + "ms, Total: " + sum + "ms " + runningTimes.get(type));
        }
    }

    private static class JarReader {
        private final Map<String, List<Long>> runningTimes;
        private final List<Path> paths;
        private final JarReaderWorker worker;
        int classes;
        public JarReader(Map<String, List<Long>> runningTimes, List<Path> paths, JarReaderWorker worker) {
            this.runningTimes = runningTimes;
            this.paths = paths;
            this.worker = worker;
        }

        void indexJar() throws IOException {
            System.gc();

            System.out.println("Scanning classpath with " + worker.getClass().getSimpleName());
            long start = System.currentTimeMillis();
            worker.beforeFullScan();
            for (Path zipFilePath : paths) {
                worker.beforeJar(zipFilePath);
                try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if entry is a directory
                        if (!entry.isDirectory()) {
                            if (entry.getName().endsWith(".class")) {
                                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                                    classes++;
                                    worker.handleClass(zipFilePath, entry.getName(), inputStream);
                                }
                            }
                        }
                    }
                }
                worker.afterJar();
            }
            worker.afterFullScan();
            long time = System.currentTimeMillis() - start;
            System.out.println("Lookup took " + time + "ms");
            System.out.println(classes + " classes found");
            System.out.println();
            List<Long> list = runningTimes.computeIfAbsent(worker.getClass().getSimpleName(), k -> new ArrayList<>());
            list.add(time);

            System.gc();
        }
    }

    private interface JarReaderWorker {
        default void beforeFullScan() throws IOException {

        }

        default void beforeJar(Path zipFilePath) throws IOException {

        }

        default void handleClass(Path zipFilePath, String classFileName, InputStream inputStream) throws IOException {

        }

        default void afterJar() throws IOException {

        }

        default void afterFullScan() throws IOException {

        }
    }

    private static class NullWorker implements JarReaderWorker {
    }

    private static class ConsumeAllBytesWorker implements JarReaderWorker {
        byte[] buf = new byte[2048];
        @Override
        public void handleClass(Path zipFilePath, String classFileName, InputStream inputStream) throws IOException {

            BufferedInputStream in = new BufferedInputStream(inputStream);
            int i = in.read(buf);
            while (i != -1) {
                i = in.read(buf);
            }
        }
    }

    private static class JandexWorker implements JarReaderWorker {
        private Indexer indexer;
        @Override
        public void beforeJar(Path zipFilePath) throws IOException {
            indexer = new Indexer();
        }

        @Override
        public void handleClass(Path zipFilePath, String classFileName, InputStream inputStream) throws IOException {
            indexer.index(inputStream);
        }

        @Override
        public void afterJar() throws IOException {
            indexer.complete();
        }
    }

    private static class FastScannerWorker implements JarReaderWorker {
        private final RuntimeIndex runtimeIndex;
        private ClassInfoScanner scanner;

        Path currentJar;

        int failures;

        private FastScannerWorker(RuntimeIndex runtimeIndex) {
            this.runtimeIndex = runtimeIndex;
            scanner = new ClassInfoScanner(runtimeIndex);
        }

        @Override
        public void beforeJar(Path zipFilePath) throws IOException {
            currentJar = zipFilePath;
        }

        @Override
        public void handleClass(Path zipFilePath, String classFileName, InputStream inputStream) throws IOException {
            try {
                //System.out.println("---> " + zipFilePath + " " + classFileName);
                scanner.scanClass(inputStream);
            } catch (RuntimeException e) {
                //System.out.println("Error");
                //System.exit(1);
                failures++;
            }

        }

        @Override
        public void afterJar() throws IOException {
        }

        @Override
        public void afterFullScan() throws IOException {
            System.out.println("Failures:"  + failures);
        }
    }



}
