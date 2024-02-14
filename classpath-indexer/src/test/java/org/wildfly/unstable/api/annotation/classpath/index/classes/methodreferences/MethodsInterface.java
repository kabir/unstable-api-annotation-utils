package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

import org.wildfly.unstable.api.annotation.classpath.index.classes.Experimental;

import java.util.function.BiFunction;

public interface MethodsInterface {

    default  <T> T mergeThings(T a, T b, BiFunction<T, T, T> merger) {
        return merger.apply(a, b);
    }

    default String staticConcat(String a, String b) {
        return a + b;
    }

    @Experimental
    default String staticConcatWithExperimental(String a, String b) {
        return a + b;
    }

    public String instanceConcat(String a, String b);

    @Experimental
    public String instanceConcatWithExperimental(String a, String b);
}
