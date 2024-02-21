package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;

/**
 * Type of annotation usage from a scanned class.
 * Typically, the classes scanned here are user code.
 */
public enum AnnotationUsageType {
    /** A scanned class extends a class annotated with an unstable api annotation */
    EXTENDS_CLASS,
    /** A scanned class implements an interface annotated with an unstable api annotation */
    IMPLEMENTS_INTERFACE,
    /** A scanned class calls a method annotated with an unstable api annotation */
    METHOD_REFERENCE,
    /** A scanned class references a field annotated with an unstable api annotation */
    FIELD_REFERENCE,
    /** A scanned class uses a classannotated with an unstable api annotation */
    CLASS_USAGE,
    /** A scanned class uses an annotation that has been annotated with an unstable api annotation*/
    ANNOTATED_ANNOTATION_USAGE
}
