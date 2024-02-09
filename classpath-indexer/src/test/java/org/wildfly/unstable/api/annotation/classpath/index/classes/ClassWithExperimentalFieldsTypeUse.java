package org.wildfly.unstable.api.annotation.classpath.index.classes;

import java.util.List;

public class ClassWithExperimentalFieldsTypeUse {
    @ExperimentalTypeUse
    public String fieldA;
    
    @ExperimentalTypeUse
    public static String fieldB;

    List<@ExperimentalTypeUse String> fieldWithTypeAnnotation;
    String notAnnotated;
}
