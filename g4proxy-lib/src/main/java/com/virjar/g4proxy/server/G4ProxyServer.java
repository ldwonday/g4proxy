package com.virjar.g4proxy.server;

import com.virjar.g4proxy.protocol.NatMessageDecoder;
import com.virjar.g4proxy.protocol.NatMessageEncoder;
import com.virjar.g4proxy.server.client.ClientManager;

import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class G4ProxyServer {

    private int natPort;
    private AtomicBoolean started = new AtomicBoolean(false);

    @Getter
    private ClientManager clientManager;

    public G4ProxyServer(int natPort) {
        this.natPort = natPort;
        this.clientManager = new ClientManager();
    }


    public void startUp() {
        if (started.compareAndSet(false, true)) {
            startUpInternal();
        }
    }

    private void startUpInternal() {

        ServerBootstrap natServerBootStrap = new ServerBootstrap();
        NioEventLoopGroup serverBossGroup = new NioEventLoopGroup(
                0,
                new DefaultThreadFactory("natServer-boss-group" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class))
                //, NioUdtProvider.BYTE_PROVIDER
        );
        NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup(
                0,
                new DefaultThreadFactory("natServer-worker-group" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class))
                //, NioUdtProvider.BYTE_PROVIDER
        );
        natServerBootStrap.group(serverBossGroup, serverWorkerGroup)
//                .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                .option(ChannelOption.SO_BACKLOG, 10)
                .option(ChannelOption.SO_REUSEADDR, true)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new NatMessageDecoder());
                        ch.pipeline().addLast(new NatMessageEncoder());
                        ch.pipeline().addLast(new ServerIdleCheckHandler());
                        ch.pipeline().addLast(new NatServerChannelHandler(clientManager));
                    }
                });
        log.info("start netty [NAT Proxy Server] server ,port:" + natPort);
        natServerBootStrap.bind(natPort).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (!future.isSuccess()) {
                    log.error("NAT proxy server startUp Failed ", future.cause());
                    started.set(false);
                }
            }
        });

        ServerBootstrap userMapServerBootStrap = new ServerBootstrap();
        serverBossGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("userMapping-boss-group" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class)));
        serverWorkerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("userMapping-worker-group" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class)));

        userMapServerBootStrap.group(serverBossGroup, serverWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new UserMappingChannelHandler(clientManager));
                    }
                });

        clientManager.setUserMappingBootstrap(userMapServerBootStrap);
    }

    public boolean isRunning() {
        return started.get();
    }
}
