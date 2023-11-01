package org.wildfly.experimental.api.classpath.index;

import java.io.PrintWriter;
import java.util.Objects;

class AnnotatedConstructor {
    private final String className;

    private final String descriptor;


    AnnotatedConstructor(String className, String descriptor) {
        this.className = className;
        this.descriptor = descriptor;
    }

    void save(PrintWriter writer, String separator) {
        writer.println(className + separator + descriptor);
    }

    static AnnotatedConstructor parseReadLine(String s, String separator) {
        String[] arr = s.split(separator);
        if (arr.length != 2) {
            throw new IllegalArgumentException(s);
        }
        return new AnnotatedConstructor(arr[0], arr[1]);
    }

    String getClassName() {
        return className;
    }

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
