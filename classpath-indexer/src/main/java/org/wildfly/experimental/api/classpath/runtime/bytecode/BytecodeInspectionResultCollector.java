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

    public void recordFieldUsage(Set<String> annotations, String className, String fieldClass, String fieldName) {
        errors.add(new AnnotatedFieldReference(annotations, className, fieldClass, fieldName));
    }

    public void recordMethodUsage(Set<String> annotations, String className, String methodClass, String methodName, String descriptor) {
        errors.add(new AnnotatedMethodReference(annotations, className, methodClass, methodName, descriptor));
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

    private class AnnotatedFieldReference extends AnnotationUsage {
        private final String className;
        private final String fieldClass;
        private final String fieldName;

        public AnnotatedFieldReference(Set<String> annotations, String className, String fieldClass, String fieldName) {
            super(annotations);
            this.className = className;
            this.fieldClass = fieldClass;
            this.fieldName = fieldName;
        }
    }

    private class AnnotatedMethodReference extends AnnotationUsage {
        private final String className;
        private final String methodClass;
        private final String methodName;
        private final String descriptor;

        public AnnotatedMethodReference(Set<String> annotations, String className, String methodClass, String methodName, String descriptor) {
            super(annotations);
            this.className = className;
            this.methodClass = methodClass;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }
    }
}
