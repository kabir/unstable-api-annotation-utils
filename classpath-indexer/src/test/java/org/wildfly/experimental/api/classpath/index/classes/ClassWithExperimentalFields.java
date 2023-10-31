package org.wildfly.experimental.api.classpath.index.classes;

public class ClassWithExperimentalFields {
    @Experimental
    String fieldA;
    
    @Experimental
    static String fieldB;

    String notAnnotated;
}
