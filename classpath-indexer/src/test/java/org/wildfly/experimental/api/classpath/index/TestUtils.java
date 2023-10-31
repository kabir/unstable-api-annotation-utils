package org.wildfly.experimental.api.classpath.index;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {
    static File createJar(Class<?>... classes) throws IOException {
        String archiveName = System.currentTimeMillis() + ".jar";
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, archiveName);
        archive.addClasses(classes);
        ZipExporter exporter = archive.as(ZipExporter.class);

        Path path = Paths.get("target/test-archives");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        path = path.resolve(archiveName);
        if (Files.exists(path)) {
            Files.delete(path);
        }

        File file = path.toFile();
        exporter.exportTo(file);
        file.deleteOnExit();
        return file;
    }
}
