package org.wildfly.experimental.api.classpath.index.classes;

public interface InterfaceWithExperimentalMethods {
    @Experimental
    public void test();
    @Experimental
    public void test(String s);

    void methodWithExperimentalParameter(@Experimental String s);

    public void notAnnotated();
}
