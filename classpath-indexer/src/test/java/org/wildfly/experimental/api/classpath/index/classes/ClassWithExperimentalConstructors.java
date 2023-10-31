package org.wildfly.experimental.api.classpath.index.classes;

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
