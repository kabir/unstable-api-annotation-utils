package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

public class StaticMethodReferenceUsage {
    public static void main(String[] args) {
        // Reference to static methods
        System.out.println(MethodsClass.mergeThings("Hello", "World", MethodsClass::staticConcat));
        System.out.println(MethodsClass.mergeThings("Hello", "World", MethodsClass::staticConcatWithExperimental));
    }
}
