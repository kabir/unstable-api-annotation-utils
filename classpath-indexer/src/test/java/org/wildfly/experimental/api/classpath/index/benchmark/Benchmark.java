package org.wildfly.experimental.api.classpath.index.benchmark;

import org.wildfly.experimental.api.classpath.index.RuntimeIndex;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;

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
        ClassBytecodeInspector inspector;
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile.toFile()))) {
            List<URL> list = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                list.add(indexDir.resolve(line).toUri().toURL());
                line = reader.readLine();
            }
            inspector = new ClassBytecodeInspector(RuntimeIndex.load(list));
        }

        JarReader jarReader = new JarReader(classpath, inspector);
        jarReader.indexJar();

    }

    private static class JarReader {
        private final List<Path> paths;
        private final ClassBytecodeInspector inspector;
        int classes;

        public JarReader(List<Path> paths, ClassBytecodeInspector inspector) {
            this.paths = paths;
            this.inspector = inspector;
        }

        void indexJar() throws IOException {
            long start = System.currentTimeMillis();
            for (Path zipFilePath : paths) {
                try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        // Check if entry is a directory
                        if (!entry.isDirectory()) {
                            if (entry.getName().endsWith(".class")) {
                                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                                    classes++;
                                    if (inspector != null) {
                                        inspector.scanClassFile(inputStream);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            long end = System.currentTimeMillis();
            if (inspector == null) {
                System.out.println("Scanning classpath with no index lookup took " + (end - start) + "ms");
            } else {
                System.out.println("Scanning classpath with index lookup took " + (end - start)  + "ms");
            }
            System.out.println(classes + " classes found");
        }
    }
}
