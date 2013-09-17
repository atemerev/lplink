package com.olfa.b2b.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FixEncoder extends MessageToByteEncoder<FixSpan> {
    @Override
    protected void encode(ChannelHandlerContext ctx, FixSpan msg, ByteBuf out) throws Exception {
        out.writeBytes(msg.encode());
    }
}
