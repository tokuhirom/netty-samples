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
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * LineBasedFrameDecoder, StringDecoder, StringEncoder を利用した echo server の実装。
 */
// デコード処理を pipeline 中で行ってくれるのでスマートに処理できる。
@Slf4j
public class LineBasedEchoServer {
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
                                new LineBasedFrameDecoder(1024),
                                new StringDecoder(StandardCharsets.UTF_8),
                                new StringEncoder(StandardCharsets.UTF_8),
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
            String s = (String) msg;
            log.info("[{}] Server received: {}({} bytes)", ctx.channel().remoteAddress(),
                    URLEncoder.encode(s, "UTF-8"), s.length());
            ctx.writeAndFlush(s + "\n");
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
