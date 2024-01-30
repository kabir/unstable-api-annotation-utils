package org.wildfly.experimental.api.classpath.index.classes;

import java.util.List;

public class ClassWithExperimentalConstructorsTypeUse {
    @ExperimentalTypeUse
    public ClassWithExperimentalConstructorsTypeUse() {

    }

    @ExperimentalTypeUse
    public ClassWithExperimentalConstructorsTypeUse(String s) {

    }

    public ClassWithExperimentalConstructorsTypeUse(@ExperimentalTypeUse int i) {

    }

    public ClassWithExperimentalConstructorsTypeUse(List<@ExperimentalTypeUse String> list) {
    }

    public ClassWithExperimentalConstructorsTypeUse(Long l) {

    }
}
