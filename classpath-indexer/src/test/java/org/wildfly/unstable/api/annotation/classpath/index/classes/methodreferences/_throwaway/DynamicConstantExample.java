package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences._throwaway;

import java.lang.reflect.Method;
/*
public class DynamicConstantExample {

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        CallSite callSite = invokedynamic(bootstrap(lookup), Object.class, "Hello, Dynamic Constant!");

        // Perform the dynamic invocation
        callSite.dynamicInvoker().invoke();
    }

    private static CallSite invokedynamic(MethodHandle bootstrap, Class<?> returnType, String constantValue) throws Throwable {
        return new ConstantCallSite(bootstrap);
    }

    private static MethodHandle bootstrap(MethodHandles.Lookup lookup) throws Throwable {
        MethodType methodType = MethodType.methodType(String.class);
        MethodHandle constantMethod = lookup.findStatic(DynamicConstantExample.class, "constantBootstrap", methodType);
        return MethodHandles.constant(String.class, constantMethod.invoke());
    }

    private static String constantBootstrap() {
        return "Dynamic Constant Value";
    }
}
*/



public class DynamicConstantExample {

    public static void main(String[] args) throws Throwable {
        Method constantMethod = DynamicConstantExample.class.getDeclaredMethod("constantBootstrap");
        constantMethod.setAccessible(true);

        Object constantValue = constantMethod.invoke(null);
        System.out.println(constantValue);
    }

    private static String constantBootstrap() {
        return "Dynamic Constant Value";
    }
}

/*
public class DynamicConstantExample {

    public static void main(String[] args) {
        DynamicConstantSupplier supplier = () -> "Dynamic Constant Value";
        System.out.println(supplier.get());
    }

    interface DynamicConstantSupplier {
        String get();
    }
}*/