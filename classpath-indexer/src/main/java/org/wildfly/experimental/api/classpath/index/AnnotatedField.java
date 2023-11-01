package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

class AnnotatedField {
    private final String className;

    private final String fieldName;

    AnnotatedField(String className, String fieldName) {
        this.className = className;
        this.fieldName = fieldName;
    }

    static AnnotatedField parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 2) {
            throw new IllegalArgumentException(s);
        }
        return new AnnotatedField(arr[0], arr[1]);
    }

    String getClassName() {
        return className;
    }

    String getFieldName() {
        return fieldName;
    }

    void save(PrintWriter writer, String separator) {
        writer.println(className + separator + fieldName);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedField)) return false;
        AnnotatedField that = (AnnotatedField) o;
        return Objects.equals(className, that.className) && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, fieldName);
    }
}
