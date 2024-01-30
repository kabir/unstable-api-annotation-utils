package org.wildfly.experimental.api.classpath.index.classes;

import java.util.List;

public class ClassWithExperimentalMethodsTypeUse {
    @ExperimentalTypeUse
    public void test() {

    }

    @ExperimentalTypeUse
    public static void test(String s) {

    }

    public void methodWithExperimentalParameter(@ExperimentalTypeUse String s) {

    }

    public void methodWithExperimentalTypeParameter(List<@ExperimentalTypeUse String> list) {

    }

    public List<@ExperimentalTypeUse String> methodWithExperimentalTypeReturn() {
        return null;
    }

    public void notAnnotated() {

    }
}
