package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.Set;

public interface AnnotationUsageReporter {
    Set<AnnotationUsage> getUsages();
    boolean checkAnnotationIndex(JandexIndex annotationIndex);
}
