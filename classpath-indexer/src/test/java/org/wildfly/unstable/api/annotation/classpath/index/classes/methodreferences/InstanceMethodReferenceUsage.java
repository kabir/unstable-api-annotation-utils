package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

public class InstanceMethodReferenceUsage {
    public static void main(String[] args) {
        // Reference to instance methods
        MethodsClass methods = new MethodsClass();
        System.out.println(MethodsClass.mergeThings("Hello", "World", methods::instanceConcat));
        System.out.println(MethodsClass.mergeThings("Hello", "World", methods::instanceConcatWithExperimental));
    }
}
