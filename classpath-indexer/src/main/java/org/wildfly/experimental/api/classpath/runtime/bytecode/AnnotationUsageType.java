package org.wildfly.experimental.api.classpath.runtime.bytecode;

/**
 * Type of annotation usage from a scanned class.
 * Typically, the classes scanned here are user code.
 */
public enum AnnotationUsageType {
    /** A scanned class extends a class annotated with an 'experimental' annotation */
    EXTENDS_CLASS,
    /** A scanned class implements an interface annotated with an 'experimental' annotation */
    IMPLEMENTS_INTERFACE,
    /** A scanned class calls a method annotated with an 'experimental' annotation */
    METHOD_REFERENCE,
    /** A scanned class references a field annotated with an 'experimental' annotation */
    FIELD_REFERENCE,
    /** A scanned class uses a classannotated with an 'experimental' annotation */
    CLASS_USAGE,

    /** A scanned class is annotated with an 'experimental' annotation */
    ANNOTATED_USER_CLASS,

    /** A method in a scanned class is annotated with an 'experimental' annotation */
    ANNOTATED_USER_METHOD,

    /** A field in a scanned class is annotated with an 'experimental' annotation */
    ANNOTATED_USER_FIELD
}
