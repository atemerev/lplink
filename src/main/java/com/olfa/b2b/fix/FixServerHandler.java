package com.olfa.b2b.fix;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;

public class FixServerHandler extends ChannelInboundHandlerAdapter {

    private final FixMessageListener listener;

    public FixServerHandler(FixMessageListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FixSpan span = (FixSpan) msg;
        System.out.println("<< " + span);
        List<FixSpan> responses = listener.onFixMessage(span);
        for (FixSpan response : responses) {
            System.out.println(">> " + response);
            ctx.write(response);
            ctx.flush();
        }
    }
}
