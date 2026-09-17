package com.c2c.message.netty;

import com.c2c.message.repository.ChatMessageRepository;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
// 🚨 新增导入 Config 类
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyServer {

    @Value("${netty.port:9090}")
    private int port;

    @Resource
    private ChatMessageRepository chatMessageRepository;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new ChunkedWriteHandler());
                                pipeline.addLast(new HttpObjectAggregator(1024 * 64));

                                // ==========================================
                                // 🚨 架构师终极修复：使用 Config 开启前缀模糊匹配！
                                // ==========================================
                                WebSocketServerProtocolConfig wsConfig = WebSocketServerProtocolConfig.newBuilder()
                                        .websocketPath("/ws")
                                        .checkStartsWith(true) // 👈 核心：允许 /ws 后面携带 ?token=xxx 等参数
                                        .build();
                                pipeline.addLast(new WebSocketServerProtocolHandler(wsConfig));

                                pipeline.addLast(new ChatHandler(chatMessageRepository));
                            }
                        });

                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("====== 🚀 Netty WebSocket 服务器启动成功 ======");
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty 启动失败", e);
            }
        }).start();
    }

    @PreDestroy
    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}