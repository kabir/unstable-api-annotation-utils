package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * <p>Internal use in the index. Contains information about a constructor annotated with one of the annotations
 * are searching for.</p>
 * <p>Found constructors will either:
 * <ul>
 *     <li>Be annotated with the searched annotation</li>
 *     <li>Have a parameter annotated with the searched annotation</li>
 *     <li>Have a parameter whose type is annotated with the searched annotation</li>
 * </ul>
 * The tool does not differentiate between the above cases. It is enough to know the constructor contains
 * the searched annotation <b>somewhere</b>.
 * </p>
 */
class AnnotatedConstructor {
    private final String className;

    private final String descriptor;


    AnnotatedConstructor(String className, String descriptor) {
        this.className = className;
        this.descriptor = descriptor;
    }

    /**
     * Called internally to save the annotated constructor to the index file
     *
     * @param writer the PrintWriter
     * @param separator the separator between the serialized parts
     */
    void save(PrintWriter writer, String separator) {
        writer.println(className + separator + descriptor);
    }

    /**
     * Reads the annotated constructor from the current line of the index file
     * @param s the line
     * @param separator the separator between the serialized parts
     * @return the parsed annotated constructor
     */
    static AnnotatedConstructor parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 2) {
            throw new IllegalArgumentException(s);
        }
        return new AnnotatedConstructor(arr[0], arr[1]);
    }

    /**
     * Gets the name of the class containing the constructor annotated with the searched annotation.
     * @return The name of the class
     */
    String getClassName() {
        return className;
    }

    /**
     * Gets the descriptor (i.e. signature) of the constructor annotated with the searched annotation.
     * @return The descriptor
     */
    String getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedConstructor)) return false;
        AnnotatedConstructor that = (AnnotatedConstructor) o;
        return Objects.equals(className, that.className) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, descriptor);
    }
}
