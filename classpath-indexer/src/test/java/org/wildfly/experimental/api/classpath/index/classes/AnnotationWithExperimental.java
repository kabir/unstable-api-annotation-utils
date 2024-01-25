package org.wildfly.experimental.api.classpath.index.classes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;

@Target({METHOD, CONSTRUCTOR, TYPE, FIELD, PARAMETER, TYPE_USE})
@Experimental
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationWithExperimental {
}
