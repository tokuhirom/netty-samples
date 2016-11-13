package com.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * もっとも基本的な echo server の実装。
 */
@Slf4j
public class BasicEchoServer {
    public static void main(String[] args) throws InterruptedException {
        int port = 3000;

        NioEventLoopGroup group = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new EchoServerHandler());
                    }
                });
        try {
            ChannelFuture f = bootstrap.bind().syncUninterruptibly();
            log.info("Listening: {}", f.channel().localAddress());
            f.channel().closeFuture().syncUninterruptibly();
        } finally {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }

    @Slf4j
    public static class EchoServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf in = (ByteBuf) msg;
            String s = in.toString(StandardCharsets.UTF_8);
            log.info("[{}] Server received: {}({} bytes)", ctx.channel().remoteAddress(),
                    URLEncoder.encode(s, "UTF-8"), s.length());
            ctx.writeAndFlush(msg);
        }


        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            log.info("[{}] channelReadComplete", ctx.channel().remoteAddress());
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            log.info("[{}] channelRegistered", ctx.channel().remoteAddress());
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            log.info("[{}] Channel unregistered", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,
                                    Throwable cause) {
            log.info("Caught unhandled exception", cause);
            ctx.close();
        }
    }
}
