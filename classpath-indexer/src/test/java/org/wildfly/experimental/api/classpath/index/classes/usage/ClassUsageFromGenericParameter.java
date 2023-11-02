package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.ClassWithExperimental;

import java.util.ArrayList;
import java.util.List;

public class ClassUsageFromGenericParameter {
    public void test() {
            List<ClassWithExperimental> list = new ArrayList<>();
    }
}
