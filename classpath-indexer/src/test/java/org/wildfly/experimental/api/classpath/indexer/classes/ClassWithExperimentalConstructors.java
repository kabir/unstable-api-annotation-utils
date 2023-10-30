package org.wildfly.experimental.api.classpath.indexer.classes;

public class ClassWithExperimentalConstructors {
    @Experimental
    public ClassWithExperimentalConstructors() {

    }

    @Experimental
    public ClassWithExperimentalConstructors(String s) {

    }

    public ClassWithExperimentalConstructors(Long l) {

    }
}
