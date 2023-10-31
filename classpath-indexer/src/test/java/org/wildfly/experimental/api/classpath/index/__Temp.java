package org.wildfly.experimental.api.classpath.index;

import org.wildfly.experimental.api.classpath.index.classes.usage.SimpleTestUsage;
import org.wildfly.experimental.api.classpath.runtime.bytecode.ClassBytecodeInspector;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class __Temp {

    public static void main(String[] args) throws IOException {
        String classLocation = SimpleTestUsage.class.getName().replaceAll("\\.", "/") + ".class";
        URL url = __Temp.class.getClassLoader().getResource(classLocation);
        try (InputStream in = url.openStream()) {
            ClassBytecodeInspector inspector = ClassBytecodeInspector.parseClassFile(in);
            System.out.println("Test");
        }
    }
}
