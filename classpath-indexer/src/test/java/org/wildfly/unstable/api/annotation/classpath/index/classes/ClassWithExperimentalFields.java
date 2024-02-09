package org.wildfly.unstable.api.annotation.classpath.index.classes;

public class ClassWithExperimentalFields {
    @Experimental
    public String fieldA;
    
    @Experimental
    public static String fieldB;

    String notAnnotated;
}
