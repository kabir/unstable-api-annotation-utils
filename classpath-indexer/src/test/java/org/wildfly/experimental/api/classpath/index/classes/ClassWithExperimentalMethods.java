package org.wildfly.experimental.api.classpath.index.classes;

public class ClassWithExperimentalMethods {
    @Experimental
    public void test() {

    }

    @Experimental
    public static void test(String s) {

    }

    public void methodWithExperimentalParameter(@Experimental String s) {

    }

    public void notAnnotated() {

    }
}
