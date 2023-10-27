package org.wildfly.experimental.api.classpath.indexer.classes;

public class ClassWithExperimentalFields {
    @Experimental
    String fieldA;
    
    @Experimental
    String fieldB;

    String notAnnotated;
}
