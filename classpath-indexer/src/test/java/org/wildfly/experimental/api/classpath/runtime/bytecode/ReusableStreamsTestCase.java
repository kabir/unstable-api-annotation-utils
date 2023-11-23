package org.wildfly.experimental.api.classpath.runtime.bytecode;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ReusableStreamsTestCase {
    @Test
    public void testReusableStreams() throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bout);
        out.writeUTF("Hi!");

        ReusableStreams reusableStreams = new ReusableStreams();
        DataInputStream in = reusableStreams.getDataInputStream(bout.toByteArray(), 0, 5);
        String s = in.readUTF();
        Assert.assertEquals("Hi!", s);

    }
}
