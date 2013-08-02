package com.olfa.fast;

import org.openfast.examples.MessageBlockReaderFactory;
import org.openfast.examples.OpenFastExample;
import org.openfast.session.Endpoint;
import org.openfast.session.FastConnectionException;
import org.openfast.session.multicast.MulticastClientEndpoint;

import java.io.File;
import java.io.IOException;

public class FastExample {

    public static void main(String[] args) throws Exception {
        Endpoint endpoint = new MulticastClientEndpoint(55000, "239.192.112.3", "172.20.200.42");
        File templatesFile = new File("./FIX50SP2-ALL.xml");
        final int readOffset = 0;
        final OpenFastExample.Variant variant = OpenFastExample.Variant.DEFAULT;
        final boolean shouldResetOnEveryMessage = false;
        final MessageBlockReaderFactory msgBlockReaderFactory = new MessageBlockReaderFactory(variant, readOffset, true);
        FastMessageConsumer consumer = new FastMessageConsumer(endpoint, templatesFile, msgBlockReaderFactory, shouldResetOnEveryMessage);

        try {
            consumer.start();
        } catch (FastConnectionException e) {
            e.printStackTrace();
            System.out.println("Unable to connect to endpoint: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An IO error occurred while consuming messages: " + e.getMessage());
            System.exit(1);
        }
    }
}
