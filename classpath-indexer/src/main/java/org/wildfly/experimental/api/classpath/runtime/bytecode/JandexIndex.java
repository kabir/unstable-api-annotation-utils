package org.wildfly.experimental.api.classpath.runtime.bytecode;


import org.jboss.jandex.AnnotationInstance;

import java.util.Collection;

/**
 * An wrapper around a Jandex annotation index
 */
public interface JandexIndex {
    Collection<AnnotationInstance> getAnnotations(final String annotationName);
}
