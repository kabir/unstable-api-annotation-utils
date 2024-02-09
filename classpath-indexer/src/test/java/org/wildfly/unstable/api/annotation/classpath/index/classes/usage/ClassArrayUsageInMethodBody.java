package org.wildfly.unstable.api.annotation.classpath.index.classes.usage;

import org.wildfly.unstable.api.annotation.classpath.index.classes.InterfaceWithExperimental;

public class ClassArrayUsageInMethodBody {
    public void test() {
        InterfaceWithExperimental[] arr = new InterfaceWithExperimental[0];
    }
}
