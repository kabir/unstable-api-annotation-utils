package org.wildfly.experimental.api.classpath.index.classes;

import java.util.List;

public class ClassWithExperimentalFieldsTypeUse {
    @ExperimentalTypeUse
    public String fieldA;
    
    @ExperimentalTypeUse
    public static String fieldB;

    List<@ExperimentalTypeUse String> fieldWithTypeAnnotation;
    String notAnnotated;
}
