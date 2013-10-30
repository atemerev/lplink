package com.olfa.b2b.fix;

import com.olfa.b2b.domain.CurrencyPair;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.lp.LpManager;
import com.olfa.b2b.shell.Shell;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class FixServer {

    private final FixMessageListener listener;
    private final int port;

    public FixServer(int port, FixMessageListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new FixDecoder(), new FixEncoder(), new FixServerHandler(listener));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 6440;
        }
        Set<Subscription> subs = new HashSet<>();
        subs.add(new Subscription("rbs", new CurrencyPair("EUR/USD"), 1000000, null));
        subs.add(new Subscription("rbs", new CurrencyPair("EUR/USD"), 3000000, null));
        subs.add(new Subscription("rbs", new CurrencyPair("EUR/USD"), 5000000, null));
        subs.add(new Subscription("rbs", new CurrencyPair("GBP/USD"), 1000000, null));
        subs.add(new Subscription("rbs", new CurrencyPair("GBP/USD"), 3000000, null));
        subs.add(new Subscription("rbs", new CurrencyPair("GBP/USD"), 5000000, null));
//        subs.add(new Subscription("bnp", new CurrencyPair("USD/JPY"), 1000000, "BLUE"));
//        subs.add(new Subscription("bnp", new CurrencyPair("USD/JPY"), 5000000, "BLUE"));
        LpManager lpManager = new LpManager(subs, 1000, 30000);
        lpManager.start();
        Shell shell = new Shell(lpManager);
        shell.start();
        FixLpManagerListener server = new FixLpManagerListener(lpManager);
        new FixServer(port, server).run();
    }
}
