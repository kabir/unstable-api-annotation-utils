package org.wildfly.experimental.api.classpath.index.benchmark;

import org.jboss.jandex.Indexer;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;
import org.wildfly.experimental.api.classpath.index.RuntimeIndex;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;
import org.wildfly.experimental.api.classpath.runtime.bytecode.JandexCollector;

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
import java.util.List;
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
        ByteRuntimeIndex byteRuntimeIndex;
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile.toFile()))) {
            List<URL> list = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                list.add(indexDir.resolve(line).toUri().toURL());
                line = reader.readLine();
            }
            runtimeIndex = RuntimeIndex.load(list);
            byteRuntimeIndex = ByteRuntimeIndex.load(list);
        }


        final int max = 50;
        for (int i = 0; i < max; i++) {
            System.out.println("==== Iteration " + i);
            new JarReader(classpath, new NullWorker()).indexJar();
            new JarReader(classpath, new ConsumeAllBytesWorker()).indexJar();
            new JarReader(classpath, new InspectorWorker(runtimeIndex)).indexJar();
            new JarReader(classpath, new JandexWorker()).indexJar();
            new JarReader(classpath, new JandexCollectorWorker(byteRuntimeIndex)).indexJar();
        }
    }

    private static class JarReader {
        private final List<Path> paths;
        private final JarReaderWorker worker;
        int classes;
        public JarReader(List<Path> paths, JarReaderWorker worker) {
            this.paths = paths;
            this.worker = worker;
        }

        void indexJar() throws IOException {
            System.gc();


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
                                    worker.handleClass(inputStream);
                                }
                            }
                        }
                    }
                }
                worker.afterJar();
            }
            worker.afterFullScan();
            long end = System.currentTimeMillis();
            System.out.println("Scanning classpath with " + worker.getClass().getSimpleName() + " lookup took " + (end - start) + "ms");
            System.out.println(classes + " classes found");
            System.out.println();

            System.gc();
        }
    }

    private interface JarReaderWorker {
        default void beforeFullScan() throws IOException {

        }

        default void beforeJar(Path zipFilePath) throws IOException {

        }

        default void handleClass(InputStream inputStream) throws IOException {

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
        public void handleClass(InputStream inputStream) throws IOException {

            BufferedInputStream in = new BufferedInputStream(inputStream);
            int i = in.read(buf);
            while (i != -1) {
                i = in.read(buf);
            }
        }
    }

    private static class InspectorWorker implements JarReaderWorker {
        private final ClassBytecodeInspector inspector;

        public InspectorWorker(RuntimeIndex runtimeIndex) {
            this.inspector = new ClassBytecodeInspector(runtimeIndex);
        }

        @Override
        public void handleClass(InputStream inputStream) throws IOException {
            inspector.scanClassFile(inputStream);
        }
    }

    private static class JandexWorker implements JarReaderWorker {
        private Indexer indexer;
        @Override
        public void beforeJar(Path zipFilePath) throws IOException {
            indexer = new Indexer();
        }

        @Override
        public void handleClass(InputStream inputStream) throws IOException {
            indexer.index(inputStream);
        }

        @Override
        public void afterJar() throws IOException {
            indexer.complete();
        }
    }


    private static class JandexCollectorWorker implements JarReaderWorker {
        private final ByteRuntimeIndex runtimeIndex;
        private Indexer indexer;
        private JandexCollector jandexCollector;

        Path currentJar;

        private JandexCollectorWorker(ByteRuntimeIndex runtimeIndex) {
            this.runtimeIndex = runtimeIndex;
            jandexCollector = new JandexCollector(runtimeIndex);
        }

        @Override
        public void beforeJar(Path zipFilePath) throws IOException {
            currentJar = zipFilePath;
            indexer = new Indexer(jandexCollector);
        }

        @Override
        public void handleClass(InputStream inputStream) throws IOException {
            indexer.index(inputStream);
        }

        @Override
        public void afterJar() throws IOException {
            indexer.complete();
            if (!jandexCollector.errorClasses.isEmpty()) {
                System.err.println(jandexCollector.errorClasses);
                throw new IllegalStateException("Errors in " + currentJar + " " + jandexCollector.errorClasses.size());
            }
        }

        @Override
        public void afterFullScan() throws IOException {


        }
    }

}
