package org.wildfly.unstable.api.annotation.classpath.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>Takes JarAnnotationIndex entries, which are scans from individual classpath entries and
 * consolidates them to an overall index. Each annotation we have searched for has its own
 * index containing the locations where that annotation was found.</p>
 *
 * <p>This overall index can be serialized and deserialized.</p>
 */
public class OverallIndex {
    private final Map<String, AnnotationIndex> indexes;

    /**
     * Creates a new overall index
     */
    public OverallIndex() {
        this.indexes = new HashMap<>();
    }

    private OverallIndex(Map<String, AnnotationIndex> indexes) {
        this.indexes = indexes;
    }

    /**
     * Scans a jar and adds its annotations to our overall index
     * @param jar the jar to scan
     * @param annotation the annotation we are searching for
     * @throws IOException if there were problems reading the jar
     */
    public void scanJar(File jar, String annotation) throws IOException {
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(jar, annotation, Collections.emptySet());
        JarAnnotationIndex jarAnnotationIndex = indexer.scanForAnnotation();
        mergeAnnotationIndex(jarAnnotationIndex);
    }

    /**
     * Scans a jar and adds its annotations to our overall index
     * @param jar the jar to scan
     * @param annotation the annotation we are searching for
     * @param excludedClasses names of classes that we should not scan
     * @throws IOException if there were problems reading the jar
     */
    public void scanJar(File jar, String annotation, Set<String> excludedClasses) throws IOException {
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(jar, annotation, excludedClasses);
        JarAnnotationIndex jarAnnotationIndex = indexer.scanForAnnotation();
        mergeAnnotationIndex(jarAnnotationIndex);
    }


    private void merge(OverallIndex index) {
        for (AnnotationIndex ai : index.indexes.values()) {
            mergeAnnotationIndex(ai);
        }
    }

    private void mergeAnnotationIndex(AnnotationIndex annotationIndex) {
        AnnotationIndex index = indexes.get(annotationIndex.getAnnotationName());
        if (index == null) {
            index = new AnnotationIndex(annotationIndex);
            indexes.put(annotationIndex.getAnnotationName(), index);
        } else {
            index.addIndexEntries(annotationIndex);
        }
    }

    /**
     * Saves the overall index to a file
     *
     * @param path location of the file we will write to
     * @throws IOException if there was an error writing to the file
     */
    public void save(Path path) throws IOException {

        Format format = determineFormat(path.getFileName().toString());
        if (format == null) {
            throw new IllegalArgumentException("Suffix of file should be .txt or .zip");
        }
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            Files.delete(path);
        }
        if (format == Format.TEXT) {
            Files.createFile(path);
            saveIndex(path);
        } else {
            Path tempIndex = Files.createTempFile("working", ".txt");
            try {
                saveIndex(tempIndex);

                URI jarUri = URI.create("jar:file:" + path.normalize().toAbsolutePath().toUri().getPath());
                Map<String, String> env = Map.of("create", "true");
                try (FileSystem jar = FileSystems.newFileSystem(jarUri, env)) {
                    Path jarPath = jar.getPath("index.txt");
                    Files.copy(tempIndex, jarPath);
                }
            } finally {
                Files.delete(tempIndex);
            }
        }
    }

    private void saveIndex(Path path) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(path.toFile())))) {
            for (AnnotationIndex annotationIndex : indexes.values()) {
                annotationIndex.save(writer);
            }
        }
    }

    /**
     * Gets the annotations we have searched for to build up the overall index
     * @return the annotations
     */
    public Set<String> getAnnotations() {
        return indexes.keySet();
    }

    /**
     * Gets the index of locations where we have searched for a particular annotation
     * @param annotation
     * @return
     */
    public AnnotationIndex getAnnotationIndex(String annotation) {
        return indexes.get(annotation);
    }

    /**
     * Loads the overall index from a file containing a serialized index, and creates an OverallIndex instance with the information.
     * @param path the location of the file
     * @param additional additional file locations
     * @return the created overall index
     * @throws IOException if there are problems reading any of the files
     */
    static OverallIndex load(Path path, Path... additional) throws IOException {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new FileNotFoundException(path.toString());
        }
        OverallIndex index = loadIndex(path);

        for (Path additionalPath : additional) {
            index.merge(loadIndex(additionalPath));
        }

        return index;
    }

    /**
     * Loads the overall index from locations specified as URLs, and creates an OverallIndex instance with the information.
     * The URLs should point to locations containing a serialized index.
     * @param urls the urls containing serialized indexes
     * @return the created overall index
     * @throws IOException if there are problems reading any of the URLs
     */
    static OverallIndex load(List<URL> urls) throws IOException {
        OverallIndex index = null;
        for (URL url : urls) {
            if (index == null) {
                index = loadIndex(url);
            } else {
                index.merge(loadIndex(url));
            }
        }
        return index;
    }


    private static OverallIndex loadIndex(Path path) throws IOException {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new FileNotFoundException(path.toString());
        }
        return loadIndex(path.toUri().toURL());
    }

    private static OverallIndex loadIndex(URL url) throws IOException {
        Format format = determineFormat(url.getFile());
        if (format == Format.ZIP) {
            // Look for index.txt in the zip file. We need to copy it to a temp file first to read it
            Path temp = Files.createTempFile("index", "zip");
            try (InputStream in = new BufferedInputStream(url.openStream())) {
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(temp.toFile()))) {
                    byte[] bytes = new byte[8192];

                    int read = in.read(bytes);
                    while (read != -1) {
                        out.write(bytes, 0, read);
                        read = in.read(bytes);
                    }
                }

                try (ZipFile zipFile = new ZipFile(temp.toFile())) {
                    ZipEntry indexEntry = zipFile.getEntry("index.txt");
                    if (indexEntry == null) {
                        throw new IllegalArgumentException(url + " does not appear to be a valid zipped index");
                    }
                    try (InputStream inputStream = zipFile.getInputStream(indexEntry)) {
                        return readFromInputStream(inputStream);
                    }
                }

            } finally {
                Files.delete(temp);
            }
        }
        return readFromInputStream(url.openStream());
    }

    private static OverallIndex readFromInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            Map<String, AnnotationIndex> indexes = AnnotationIndex.loadAll(reader);
            return new OverallIndex(indexes);
        }
    }


    private static String determineSuffix(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return null;
        }
        return fileName.substring(index);
    }
    private static Format determineFormat(String fileName) {
        String suffix = determineSuffix(fileName);
        return Format.find(suffix);
    }

    /**
     * For testing
     * @param o the other index
     * @return true if the instances are the same or have the same contents
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OverallIndex)) return false;
        OverallIndex that = (OverallIndex) o;
        return Objects.equals(indexes, that.indexes);
    }

    public enum Format {
        TEXT(".txt"),
        ZIP(".zip");

        private final String suffix;

        Format(String suffix) {
            this.suffix = suffix;
        }

        static Format find(String suffix) {
            if (suffix == null) {
                return null;
            }
            switch (suffix) {
                case ".txt":
                    return Format.TEXT;
                case ".zip":
                    return Format.ZIP;
            }
            return null;
        }
    }
}
