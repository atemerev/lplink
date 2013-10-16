package com.olfa.b2b.fix;

public interface ByteSerializable {

    public static final byte FIELD_DIVIDER = 0x01;
    public static final byte MESSAGE_DIVIDER = 0x02;

    byte[] encode();
}
