package org.wildfly.experimental.api.classpath.index.benchmark;

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
import org.wildfly.experimental.api.classpath.runtime.bytecode.FastClassInfoScanner;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class __TempDebug {
    public static void main(String[] args) throws Exception {
//        parseClass(
//                "/Users/kabir/.m2/repository/org/wildfly/galleon-plugins/wildfly-galleon-plugins/6.5.0.Final/wildfly-galleon-plugins-6.5.0.Final.jar",
//                "org.wildfly.galleon.plugin.MonitorableArtifact");
//        parseClass(
//                "/Users/kabir/.m2/repository/org/wildfly/galleon-plugins/wildfly-galleon-plugins/6.5.0.Final/wildfly-galleon-plugins-6.5.0.Final.jar",
//                //"org.wildfly.galleon.plugin.config.LineEndingsTask$1",
//                "org.wildfly.galleon.plugin.config.WildFlyPackageTasksParser31$Element",
//                "org.wildfly.galleon.plugin.WildFlyPackageTask$Phase");
//        parseClass(
//                "/Users/kabir/.m2/repository/org/wildfly/galleon-plugins/wildfly-galleon-plugins/6.5.0.Final/wildfly-galleon-plugins-6.5.0.Final.jar",
//                "org.wildfly.galleon.plugin.PropertyReplacer");
        parseClass("/Users/kabir/.m2/repository/jakarta/json/jakarta.json-api/2.1.3/jakarta.json-api-2.1.3.jar", "module-info");

    }

    private static void parseClass(String location, String...classNames) throws Exception {

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
        //JandexCollector collector = new JandexCollector(runtimeIndex);
        FastClassInfoScanner scanner = new FastClassInfoScanner(runtimeIndex);

        Set<String> classNameSet = new LinkedHashSet<>();
        for (String cn : classNames) {
            classNameSet.add(cn.replaceAll("\\.", "/") + ".class");
        }
        try (ZipFile zipFile = new ZipFile(Paths.get(location).toFile())) {
            //Indexer indexer = new Indexer(collector);
            try {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    // Check if entry is a directory
                    if (!entry.isDirectory()) {
                        if (classNameSet.contains(entry.getName())) {
                            System.out.println(entry.getName());
                            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                                //indexer.index(inputStream);
                                scanner.scanClass(inputStream);
                                System.out.println("--> " + scanner.getUsages());
                            }
                        }
                    }
                }
            } finally {
                //indexer.complete();
            }
        }
    }
}
