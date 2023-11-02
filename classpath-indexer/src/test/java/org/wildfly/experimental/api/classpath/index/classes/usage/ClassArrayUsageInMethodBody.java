package org.wildfly.experimental.api.classpath.index.classes.usage;

import org.wildfly.experimental.api.classpath.index.classes.InterfaceWithExperimental;

public class ClassArrayUsageInMethodBody {
    public void test() {
        InterfaceWithExperimental[] arr = new InterfaceWithExperimental[0];
    }
}
