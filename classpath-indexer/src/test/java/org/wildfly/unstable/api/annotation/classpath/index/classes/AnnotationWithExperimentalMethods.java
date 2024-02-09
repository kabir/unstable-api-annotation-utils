package org.wildfly.unstable.api.annotation.classpath.index.classes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithExperimentalMethods {
    @Experimental
    String value();
}
