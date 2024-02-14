package org.wildfly.unstable.api.annotation.classpath.index.classes.methodreferences._throwaway;

import java.lang.invoke.*;

public class DynamicConstantExample4 {

    public static void main(String[] args) {
        String dynamicConstant = createDynamicConstant();
        System.out.println(dynamicConstant);
    }

    private static String createDynamicConstant() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        CallSite callSite = invokedynamic(lookup, "ConstantBootstrap", MethodType.methodType(String.class));

        try {
            return (String) callSite.dynamicInvoker().invoke();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static CallSite invokedynamic(MethodHandles.Lookup lookup, String methodName, MethodType methodType) {
        MethodHandle bootstrap = null;

        try {
            bootstrap = lookup.findStatic(DynamicConstantExample.class, methodName, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace(); // Handle exception properly in a real scenario
        }

        return new ConstantCallSite(bootstrap);
    }

    private static String ConstantBootstrap() {
        return "Dynamic Constant Value";
    }
}