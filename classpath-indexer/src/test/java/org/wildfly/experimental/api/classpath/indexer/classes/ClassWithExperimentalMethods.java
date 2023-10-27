package org.wildfly.experimental.api.classpath.indexer.classes;

public class ClassWithExperimentalMethods {
    @Experimental
    public void test() {

    }

    @Experimental
    public void test(String s) {

    }

    public void notAnnotated() {

    }
}
