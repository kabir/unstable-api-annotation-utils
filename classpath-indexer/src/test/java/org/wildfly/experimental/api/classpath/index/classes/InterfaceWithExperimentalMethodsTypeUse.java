package org.wildfly.experimental.api.classpath.index.classes;

import java.util.List;

public interface InterfaceWithExperimentalMethodsTypeUse {
    @ExperimentalTypeUse
    public void test();
    @ExperimentalTypeUse
    public void test(String s);

    void methodWithExperimentalParameter(@ExperimentalTypeUse String s);

    void methodWithExperimentalTypeParameter(List<@ExperimentalTypeUse String> list);

    List<@ExperimentalTypeUse String> methodWithExperimentalTypeReturn();

    public void notAnnotated();
}
