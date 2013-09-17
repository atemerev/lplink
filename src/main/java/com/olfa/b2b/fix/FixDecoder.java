package com.olfa.b2b.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class FixDecoder extends ByteToMessageDecoder {

    private final FixParser parser;

    public FixDecoder() {
        this.parser = new FixParser();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf bytes = in.readBytes(in.readableBytes());
        out.addAll(parser.onData(bytes.array()));
    }
}
