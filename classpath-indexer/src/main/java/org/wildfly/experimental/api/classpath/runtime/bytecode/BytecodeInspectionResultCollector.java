package org.wildfly.experimental.api.classpath.runtime.bytecode;

import java.util.HashSet;
import java.util.Set;

public class BytecodeInspectionResultCollector {
    // TODO make a nicer format
    Set<AnnotationUsage> errors = new HashSet<>();
    void recordSuperClassUsage(Set<String> annotations, String clazz, String superClass) {
        errors.add(new ExtendsAnnotatedClass(annotations, clazz, superClass));
    }

    public void recordImplementsInterfaceUsage(Set<String> annotations, String className, String iface) {
        errors.add(new ImplementsAnnotatedInterface(annotations, className, iface));
    }


    public abstract class AnnotationUsage {
        private final Set<String> annotations;

        public AnnotationUsage(Set<String> annotations) {
            this.annotations = annotations;
        }
    }

    public class ExtendsAnnotatedClass extends AnnotationUsage {
        private final String clazz;
        private final String superClass;

        protected ExtendsAnnotatedClass(Set<String> annotations, String clazz, String superClass) {
            super(annotations);
            this.clazz = clazz;
            this.superClass = superClass;
        }
    }

    public class ImplementsAnnotatedInterface extends ExtendsAnnotatedClass {
        public ImplementsAnnotatedInterface(Set<String> annotations, String clazz, String superClass) {
            super(annotations, clazz, superClass);
        }
    }
}
