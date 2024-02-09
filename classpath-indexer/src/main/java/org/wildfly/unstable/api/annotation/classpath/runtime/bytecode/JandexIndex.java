package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;


import org.jboss.jandex.AnnotationInstance;

import java.util.Collection;

/**
 * A wrapper around a Jandex annotation index. It is used to find places in the user code
 * that use the unstable api annotations directly.
 */
public interface JandexIndex {
    Collection<AnnotationInstance> getAnnotations(final String annotationName);
}
