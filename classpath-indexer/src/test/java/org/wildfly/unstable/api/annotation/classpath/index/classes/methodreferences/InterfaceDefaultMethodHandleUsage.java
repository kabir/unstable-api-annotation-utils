package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

public class InterfaceDefaultMethodHandleUsage {
    public static void main(String[] args) {
        // Reference to instance methods
        MethodHandlesClass.Concat iface = null;
        System.out.println(MethodHandlesClass.mergeThings("Hello", "World", iface::defaultConcat));
        System.out.println(MethodHandlesClass.mergeThings("Hello", "World", iface::defaultConcatWithExperimental));
    }
}
