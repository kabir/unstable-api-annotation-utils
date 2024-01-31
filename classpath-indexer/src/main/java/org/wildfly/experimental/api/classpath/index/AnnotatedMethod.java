package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * <p>Internal use in the index. Contains information about a method annotated with one of the annotations
 * we are searching for.</p>
 * <p>Found methods will either:
 * <ul>
 *     <li>Be annotated with the searched annotation</li>
 *     <li>Have a parameter annotated with the searched annotation</li>
 *     <li>Have a parameter whose type is annotated with the searched annotation</li>
 *     <li>Have a return type whose type is annotated with the searched annotation</li>
 * </ul>
 * The tool does not differentiate between the above cases. It is enough to know the method contains
 * the searched annotation <b>somewhere</b>.
 * </p>
 */
class AnnotatedMethod {
    private final String className;
    private final String methodName;
    private final String descriptor;


    AnnotatedMethod(String className, String methodName, String descriptor) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedMethod)) return false;
        AnnotatedMethod that = (AnnotatedMethod) o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName) && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, descriptor);
    }

    /**
     * Called internally to save the annotated method to the index file
     *
     * @param writer the PrintWriter
     * @param separator the separator between the serialized parts
     */
    void save(PrintWriter writer, String separator) {
        writer.println(className + separator + methodName + separator + descriptor);
    }

    /**
     * Reads the annotated method from the current line of the index file
     * @param s the line
     * @param separator the separator between the serialized parts
     * @return the parsed annotated method
     */
    static AnnotatedMethod parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 3) {
            throw new IllegalArgumentException(s);
        }

        return new AnnotatedMethod(arr[0], arr[1], arr[2]);
    }

    /**
     * Gets the name of the class containing the method annotated with the searched annotation.
     * @return The name of the class
     */
    String getClassName() {
        return className;
    }

    /**
     * Gets the name of the method annotated with the searched annotation.
     * @return The name of the method
     */
    String getMethodName() {
        return methodName;
    }

    /**
     * Gets the descriptor (i.e. signature) of the method annotated with the searched annotation.
     * @return The descriptor
     */
    String getDescriptor() {
        return descriptor;
    }

}
