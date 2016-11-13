package com.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * もっとも基本的な HTTP server の実装。
 */
@Slf4j
public class HttpServer {
    public static void main(String[] args) throws InterruptedException {
        int port = 3000;

        NioEventLoopGroup group = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new HttpServerCodec(),
                                new HttpObjectAggregator(512 * 1024),
                                new HttpServerHandler());
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
    public static class HttpServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            FullHttpRequest request = (FullHttpRequest) msg;
            log.info("[{}] {} {} {}",
                    ctx.channel().remoteAddress(),
                    request.protocolVersion(),
                    request.method(),
                    request.uri());
            byte[] content = "Hello".getBytes(StandardCharsets.UTF_8);
            DefaultHttpResponse defaultHttpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.buffer()
                            .writeBytes(content),
                    new DefaultHttpHeaders()
                            .add(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                            .add(HttpHeaderNames.CONTENT_LENGTH, content.length),
                    EmptyHttpHeaders.INSTANCE);

            ctx.writeAndFlush(defaultHttpResponse);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,
                                    Throwable cause) {
            log.warn("Caught unhandled exception", cause);
            ctx.close();
        }
    }
}
