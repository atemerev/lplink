package com.olfa.fast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class MulticastTest {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws Exception {
        assert args.length >= 2;
        NetworkInterface ni = NetworkInterface.getByName("ppp0");
        DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(55000))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
        dc.configureBlocking(false);
        InetAddress group = InetAddress.getByName(args[0]);
        MembershipKey key = dc.join(group, ni, InetAddress.getByName(args[1]));
        System.out.println(String.format("Joined multicast group: %s", key.toString()));
        Selector selector = Selector.open();
        dc.register(selector, SelectionKey.OP_READ);
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        while (true) {
            dc.receive(buffer);
            buffer.flip();
            byte[] bytearr = new byte[buffer.remaining()];
            System.out.print(bytesToHex(bytearr));
        }
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
