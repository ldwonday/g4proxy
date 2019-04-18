package com.virjar.g4proxy.client;

import com.virjar.g4proxy.protocol.NatMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatClientChannelHandler extends SimpleChannelInboundHandler<NatMessage> {
    private G4ProxyClient g4ProxyClient;

    public NatClientChannelHandler(G4ProxyClient g4ProxyClient) {
        this.g4ProxyClient = g4ProxyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NatMessage msg) throws Exception {
        switch (msg.getType()) {
            case NatMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, msg);
                break;
            case NatMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            case NatMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, msg);
                break;
            case NatMessage.TYPE_HEARTBEAT:
                handleHeartbeatMessage();
                break;
            default:
                log.warn("Unknown message type:{}", msg.getType());
                break;
        }
    }

    private void handleHeartbeatMessage() {
        log.info("receive heartbeat message from nat server");
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, NatMessage msg) {
        long serialNumber = msg.getSerialNumber();
        if (serialNumber <= 0) {
            log.warn("the serialNumber lost");
            return;
        }

        log.info("disconnect for request:{}", serialNumber);
        g4ProxyClient.getHttpProxyConnectionManager().releaseConnection(serialNumber);
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, NatMessage msg) {
        long serialNumber = msg.getSerialNumber();
        if (serialNumber <= 0) {
            log.warn("the serialNumber lost");
            return;
        }

        Channel littelProxyChannel = g4ProxyClient.getHttpProxyConnectionManager().query(serialNumber);
        if (littelProxyChannel == null) {
            log.warn("no LITTEL proxy channel bound for request:{}", serialNumber);
            NatMessage natMessage = new NatMessage();
            natMessage.setType(NatMessage.TYPE_DISCONNECT);
            natMessage.setSerialNumber(msg.getSerialNumber());
            ctx.channel().writeAndFlush(natMessage);
            return;
        }

        log.info("forward data to littel proxy endpoint");
        ByteBuf byteBuf = ctx.alloc().buffer(msg.getData().length);
        byteBuf.writeBytes(msg.getData());
        littelProxyChannel.writeAndFlush(byteBuf);
    }

    private void handleConnectMessage(final ChannelHandlerContext ctx, final NatMessage msg) {
        int littelServerPort = LittelProxyBootstrap.getLittelServerPort();
        g4ProxyClient.getJoin2LittleProxyBootStrap().connect("127.0.0.1", littelServerPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    log.warn("connect to LITTEL proxy failed", future.cause());
                    NatMessage natMessage = new NatMessage();
                    natMessage.setType(NatMessage.TYPE_DISCONNECT);
                    natMessage.setSerialNumber(msg.getSerialNumber());
                    ctx.channel().writeAndFlush(natMessage);
                    return;
                }

                //bound
                g4ProxyClient.getHttpProxyConnectionManager().register(msg.getSerialNumber(), future.channel());
                NatMessage natMessage = new NatMessage();
                natMessage.setType(NatMessage.TYPE_CONNECT_READY);
                natMessage.setSerialNumber(msg.getSerialNumber());
                ctx.channel().writeAndFlush(natMessage);
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        g4ProxyClient.getHttpProxyConnectionManager().closeAllProxyConnection();
        g4ProxyClient.forceReconnect();
        super.channelInactive(ctx);
    }
}
