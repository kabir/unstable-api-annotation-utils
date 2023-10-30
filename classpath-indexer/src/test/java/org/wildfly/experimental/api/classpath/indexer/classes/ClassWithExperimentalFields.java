package org.wildfly.experimental.api.classpath.indexer.classes;

public class ClassWithExperimentalFields {
    @Experimental
    String fieldA;
    
    @Experimental
    static String fieldB;

    String notAnnotated;
}
