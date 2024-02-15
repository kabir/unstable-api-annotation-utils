package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

public class ClassInstanceMethodHandleUsage {
    public static void main(String[] args) {
        // Reference to instance methods
        MethodHandlesClass methods = new MethodHandlesClass();
        System.out.println(MethodHandlesClass.mergeThings("Hello", "World", methods::instanceConcat));
        System.out.println(MethodHandlesClass.mergeThings("Hello", "World", methods::instanceConcatWithExperimental));
    }
}
