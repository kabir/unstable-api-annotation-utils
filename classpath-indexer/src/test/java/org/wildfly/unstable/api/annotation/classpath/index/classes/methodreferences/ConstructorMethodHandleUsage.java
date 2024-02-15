package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

public class ConstructorMethodHandleUsage {
    public static void main(String[] args) {
        // Reference to instance methods
        System.out.println(MethodHandlesClass.createInstance(MethodHandlesClass.ClassWithStandardConstructor::new));
        System.out.println(MethodHandlesClass.createInstance(MethodHandlesClass.ClassWithExperimentalConstructor::new));
    }
}
