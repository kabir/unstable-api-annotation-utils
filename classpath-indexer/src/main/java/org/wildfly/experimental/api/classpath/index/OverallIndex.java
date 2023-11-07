package org.wildfly.experimental.api.classpath.index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OverallIndex {
    private final Map<String, AnnotationIndex> indexes;

    public OverallIndex() {
        this.indexes = new HashMap<>();
    }

    private OverallIndex(Map<String, AnnotationIndex> indexes) {
        this.indexes = indexes;
    }

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

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            Files.delete(path);
        }
        Files.createFile(path);
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(path.toFile())))) {
            for (AnnotationIndex annotationIndex : indexes.values()) {
                annotationIndex.save(writer);
            }
        }
    }

    public Set<String> getAnnotations() {
        return indexes.keySet();
    }

    public AnnotationIndex getAnnotationIndex(String annotation) {
        return indexes.get(annotation);
    }

    public static OverallIndex load(Path path, Path... additional) throws IOException {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new FileNotFoundException(path.toString());
        }
        OverallIndex index = loadIndex(path);

        for (Path additionalPath : additional) {
            index.merge(loadIndex(additionalPath));
        }

        return index;
    }

    public static OverallIndex load(List<URL> urls) throws IOException {
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            Map<String, AnnotationIndex> indexes = AnnotationIndex.loadAll(reader);
            return new OverallIndex(indexes);
        }
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
}
