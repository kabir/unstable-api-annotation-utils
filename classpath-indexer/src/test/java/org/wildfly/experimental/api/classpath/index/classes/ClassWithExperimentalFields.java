package org.wildfly.experimental.api.classpath.index.classes;

public class ClassWithExperimentalFields {
    @Experimental
    public String fieldA;
    
    @Experimental
    public static String fieldB;

    String notAnnotated;
}
