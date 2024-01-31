package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * <p>Internal use in the index. Contains information about a field annotated with one of the annotations
 * we are searching for.</p>
 * <p>Found fields will either:
 * <ul>
 *     <li>Be annotated with the searched annotation</li>
 *     <li>Have a type annotated with the searched annotation</li>
 * </ul>
 * The tool does not differentiate between the above cases. It is enough to know the method contains
 * the searched annotation <b>somewhere</b>.
 * </p>
 */
class AnnotatedField {
    private final String className;

    private final String fieldName;

    AnnotatedField(String className, String fieldName) {
        this.className = className;
        this.fieldName = fieldName;
    }

    /**
     * Reads the annotated field from the current line of the index file
     * @param s the line
     * @param separator the separator between the serialized parts
     * @return the parsed annotated field
     */
    static AnnotatedField parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 2) {
            throw new IllegalArgumentException(s);
        }
        return new AnnotatedField(arr[0], arr[1]);
    }

    /**
     * Gets the name of the class containing the field annotated with the searched annotation.
     * @return The name of the class
     */
    String getClassName() {
        return className;
    }

    /**
     * Gets the name of the field annotated with the searched annotation.
     * @return The name of the field
     */
    String getFieldName() {
        return fieldName;
    }

    /**
     * Called internally to save the annotated field to the index file
     *
     * @param writer the PrintWriter
     * @param separator the separator between the serialized parts
     */
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
