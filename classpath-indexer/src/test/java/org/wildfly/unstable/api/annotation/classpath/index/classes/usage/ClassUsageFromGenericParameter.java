package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.ClassWithExperimental;

import java.util.ArrayList;
import java.util.List;

public class ClassUsageFromGenericParameter {
    public void test() {
            List<ClassWithExperimental> list = new ArrayList<>();
    }
}
