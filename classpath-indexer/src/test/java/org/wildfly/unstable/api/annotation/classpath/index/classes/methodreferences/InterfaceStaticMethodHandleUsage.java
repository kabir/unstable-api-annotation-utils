package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

public class InterfaceStaticMethodHandleUsage {
    public static void main(String[] args) {
        // Reference to static methods
        System.out.println(MethodHandlesClass.mergeThings("Hello", "World", MethodHandlesClass.Concat::staticConcat));
        System.out.println(MethodHandlesClass.mergeThings("Hello", "World", MethodHandlesClass.Concat::staticConcatWithExperimental));
    }
}
