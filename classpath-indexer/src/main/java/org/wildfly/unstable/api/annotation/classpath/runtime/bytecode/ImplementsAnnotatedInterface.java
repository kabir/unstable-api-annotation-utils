package org.wildfly.unstable.api.annotation.classpath.runtime.bytecode;

import org.wildfly.unstable.api.annotation.classpath.index.OverallIndex;

import java.util.Objects;
import java.util.Set;

import static org.wildfly.unstable.api.annotation.classpath.index.RuntimeIndex.convertClassNameToDotFormat;
import static org.wildfly.unstable.api.annotation.classpath.runtime.bytecode.AnnotationUsageType.IMPLEMENTS_INTERFACE;

/**
 * Records a class in user bytecode that implements an interface that has been annotated with one of the annotations
 * we recorded as unstable api in the {@link OverallIndex}
 */
public class ImplementsAnnotatedInterface extends AnnotationWithSourceClassUsage {
    private final String iface;

    ImplementsAnnotatedInterface(Set<String> annotations, String clazz, String iface) {
        super(annotations, IMPLEMENTS_INTERFACE, clazz);
        this.iface = iface;
    }

    /**
     * Gets the implemented interface that has been annotated with an unstable api annotation
     * @return the interface name
     */
    public String getInterface() {
        return iface;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImplementsAnnotatedInterface that = (ImplementsAnnotatedInterface) o;
        return Objects.equals(iface, that.iface);
    }

    @Override
    // Don't override hashcode here, the root class will do some caching
    protected int calculateHash() {
        return Objects.hash(super.calculateHash(), iface);
    }

    @Override
    protected AnnotationUsage convertToDotFormat() {
        return new ImplementsAnnotatedInterface(
                annotations,
                convertClassNameToDotFormat(sourceClass),
                convertClassNameToDotFormat(iface));
    }
}
