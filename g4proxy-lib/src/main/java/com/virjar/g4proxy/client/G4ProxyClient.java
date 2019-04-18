package com.virjar.g4proxy.client;

import com.virjar.g4proxy.protocol.NatMessage;
import com.virjar.g4proxy.protocol.NatMessageDecoder;
import com.virjar.g4proxy.protocol.NatMessageEncoder;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class G4ProxyClient {

    private String natServerHost;
    private int natServerPort;
    private String clientId;

    @Getter
    private Channel natChannel;

    @Getter
    private HttpProxyConnectionManager httpProxyConnectionManager = new HttpProxyConnectionManager();

    @Getter
    private Bootstrap join2LittleProxyBootStrap;

    private Bootstrap join2NatServerBootStrap;

    public G4ProxyClient(String natServerHost, int natServerPort, String clientId) {
        this.natServerHost = natServerHost;
        this.natServerPort = natServerPort;
        this.clientId = clientId;
    }

    public void startup() {

        LittelProxyBootstrap.makeSureLittelProxyStartup();

        NioEventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("littel-proxy-group" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class)));
        join2LittleProxyBootStrap = new Bootstrap();
        join2LittleProxyBootStrap.group(workerGroup);
        join2LittleProxyBootStrap.channel(NioSocketChannel.class);
        join2LittleProxyBootStrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new LittleProxyChannelHandler(G4ProxyClient.this));
            }
        });

        workerGroup = new NioEventLoopGroup(
                0,
                new DefaultThreadFactory("nat-endpoint-group" + DefaultThreadFactory.toPoolName(NioEventLoopGroup.class))
                //, NioUdtProvider.BYTE_PROVIDER
        );
        join2NatServerBootStrap = new Bootstrap();
        join2NatServerBootStrap.group(workerGroup);
        join2NatServerBootStrap.channel(NioSocketChannel.class);
        //join2LittleProxyBootStrap.channelFactory(NioUdtProvider.BYTE_ACCEPTOR);
        join2NatServerBootStrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new NatMessageDecoder());
                ch.pipeline().addLast(new NatMessageEncoder());
                ch.pipeline().addLast(new ClientIdleCheckHandler(G4ProxyClient.this));
                ch.pipeline().addLast(new NatClientChannelHandler(G4ProxyClient.this));
            }
        });

        forceReconnect();
    }

    private volatile boolean isConnecting = false;

    void forceReconnect() {
        Channel channelCopy = natChannel;
        natChannel = null;
        if (channelCopy != null && channelCopy.isActive()) {
            channelCopy.close();
        }
        connectNatServer();
    }

    private synchronized void connectNatServer() {
        Channel cmdChannelCopy = natChannel;
        if (cmdChannelCopy != null && cmdChannelCopy.isActive()) {
            return;
        }
        if (isConnecting) {
            log.warn("connect event fire already");
            return;
        }
        isConnecting = true;
        join2NatServerBootStrap.group().submit(new Runnable() {
            @Override
            public void run() {
                log.info("connect to nat server...");
                Channel cmdChannelCopy = natChannel;
                if (cmdChannelCopy != null && cmdChannelCopy.isActive()) {
                    log.info("cmd channel active, and close channel,heartbeat timeout ?");
                    cmdChannelCopy.close();
                    //TODO clean up all resource
                }
                join2NatServerBootStrap.connect(natServerHost, natServerPort).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        isConnecting = false;
                        if (!channelFuture.isSuccess()) {
                            log.warn("connect to nat server failed", channelFuture.cause());
                            join2NatServerBootStrap.group().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    log.info("connect to nat server failed, reconnect by scheduler task start");
                                    connectNatServer();
                                }
                            }, reconnectWait(), TimeUnit.MILLISECONDS);

                        } else {
                            sleepTimeMill = 1000;
                            natChannel = channelFuture.channel();
                            log.info("connect to nat server success:{}", natChannel);

                            NatMessage proxyMessage = new NatMessage();
                            proxyMessage.setType(NatMessage.C_TYPE_REGISTER);
                            proxyMessage.setExtra(clientId);
                            natChannel.writeAndFlush(proxyMessage);
                        }
                    }
                });
            }
        });

    }

    private static long sleepTimeMill = 1000;

    private static long reconnectWait() {

        if (sleepTimeMill > 120000) {
            sleepTimeMill = 120000;
        }

        synchronized (G4ProxyClient.class) {
            sleepTimeMill = sleepTimeMill + 1000;
            return sleepTimeMill;
        }

    }
}
