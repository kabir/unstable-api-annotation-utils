package org.wildfly.experimental.api.classpath.indexer.classes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithExperimentalMethods {
    @Experimental
    String value();
}
