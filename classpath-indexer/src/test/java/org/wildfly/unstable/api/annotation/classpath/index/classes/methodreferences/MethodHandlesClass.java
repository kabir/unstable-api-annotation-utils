package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences;

import org.wildfly.unstable.api.annotation.classpath.index.classes.Experimental;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MethodHandlesClass {

    // Methods taking lamdas for use in tests
    public static <T> T mergeThings(T a, T b, BiFunction<T, T, T> merger) {
        return merger.apply(a, b);
    }

    public static <T> T createInstance(Supplier<T> supplier) {
        return supplier.get();
    }

    public static String staticConcat(String a, String b) {
        return a + b;
    }

    @Experimental
    public static String staticConcatWithExperimental(String a, String b) {
        return a + b;
    }

    public String instanceConcat(String a, String b) {
        return a + b;
    }

    @Experimental
    public String instanceConcatWithExperimental(String a, String b) {
        return a + b;
    }

    public interface Concat {
        static String staticConcat(String a, String b) {
            return a + b;
        }

        @Experimental
        static String staticConcatWithExperimental(String a, String b) {
            return a + b;
        }

        String instanceConcat(String a, String b);

        @Experimental
        String instanceConcatWithExperimental(String a, String b);

        default String defaultConcat(String a, String b) {
            return a + b;
        }

        @Experimental
        default String defaultConcatWithExperimental(String a, String b) {
            return a + b;
        }
    }

    public static class ClassWithStandardConstructor {
        ClassWithStandardConstructor() {
        }
    }

    public static class ClassWithExperimentalConstructor {
        @Experimental
        ClassWithExperimentalConstructor() {
        }
    }
}
