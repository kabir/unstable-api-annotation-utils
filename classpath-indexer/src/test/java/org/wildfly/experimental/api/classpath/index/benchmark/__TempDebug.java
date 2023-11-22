package org.wildfly.experimental.api.classpath.index.benchmark;

import org.jboss.jandex.Indexer;
import org.wildfly.experimental.api.classpath.index.ByteRuntimeIndex;
import org.wildfly.experimental.api.classpath.index.OverallIndex;
import org.wildfly.experimental.api.classpath.index.TestUtils;
import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.AnnotationWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalConstructors;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalFields;
import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.index.classes.Experimental;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimental;
import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimentalMethods;
import org.wildfly.experimental.api.classpath.runtime.bytecode.JandexCollector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class __TempDebug {
    public static void main(String[] args) throws Exception {
        parseClass(
                "/Users/kabir/.m2/repository/org/wildfly/galleon-plugins/wildfly-galleon-plugins/6.5.0.Final/wildfly-galleon-plugins-6.5.0.Final.jar",
                "org.wildfly.galleon.plugin.MonitorableArtifact");
//        parseClass(
//                "/Users/kabir/.m2/repository/org/wildfly/galleon-plugins/wildfly-galleon-plugins/6.5.0.Final/wildfly-galleon-plugins-6.5.0.Final.jar",
//                "org.wildfly.galleon.plugin.PropertyReplacer");

    }

    private static void parseClass(String location, String className) throws Exception {

        OverallIndex overallIndex = new OverallIndex();
        File file = TestUtils.createJar(
                AnnotationWithExperimental.class,
                ClassWithExperimental.class,
                InterfaceWithExperimental.class,
                ClassWithExperimentalMethods.class,
                InterfaceWithExperimentalMethods.class,
                AnnotationWithExperimentalMethods.class,
                ClassWithExperimentalConstructors.class,
                ClassWithExperimentalFields.class);
        overallIndex.scanJar(file, Experimental.class.getName(), Collections.emptySet());

        Path p = Paths.get("target/index/runtime-test");
        overallIndex.save(p);

        ByteRuntimeIndex runtimeIndex = ByteRuntimeIndex.load(p);
        JandexCollector collector = new JandexCollector(runtimeIndex);


        className = className.replaceAll("\\.", "/") + ".class";
        try (ZipFile zipFile = new ZipFile(Paths.get(location).toFile())) {
            Indexer indexer = new Indexer(collector);
            try {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    // Check if entry is a directory
                    if (!entry.isDirectory()) {
                        if (entry.getName().equals(className)) {
                            System.out.println(entry.getName());
                            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                                indexer.index(inputStream);
                            }
                        }
                    }
                }
            } finally {
                indexer.complete();
            }
        }
    }
}
