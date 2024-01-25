package org.wildfly.experimental.api.classpath.runtime.bytecode;

public enum AnnotationUsageType {
    EXTENDS_CLASS,
    IMPLEMENTS_INTERFACE,
    METHOD_REFERENCE,
    FIELD_REFERENCE,
    CLASS_USAGE,

    ANNOTATED_USER_CLASS,

    ANNOTATED_USER_METHOD,

    ANNOTATED_USER_FIELD
}
