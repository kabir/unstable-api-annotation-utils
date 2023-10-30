package org.wildfly.experimental.api.classpath.indexer.classes;

public class ClassWithExperimentalMethods {
    @Experimental
    public void test() {

    }

    @Experimental
    public static void test(String s) {

    }

    public void notAnnotated() {

    }
}
