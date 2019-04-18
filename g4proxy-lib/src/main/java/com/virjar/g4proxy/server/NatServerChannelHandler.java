package com.virjar.g4proxy.server;

import com.virjar.g4proxy.protocol.Constant;
import com.virjar.g4proxy.protocol.NatMessage;
import com.virjar.g4proxy.server.client.ClientManager;
import com.virjar.g4proxy.server.client.NatClientImage;

import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class NatServerChannelHandler extends SimpleChannelInboundHandler<NatMessage> {
    private ClientManager clientManager;

    public NatServerChannelHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NatMessage msg) throws Exception {
        log.info("received message type:{}  for request:{}", msg.getReadableType(), msg.getSerialNumber());
        switch (msg.getType()) {
            case NatMessage.TYPE_HEARTBEAT:
                handleHeartbeatMessage(ctx, msg);
                break;
            case NatMessage.C_TYPE_REGISTER:
                handleRegister(ctx, msg);
                break;
//            case NatMessage.TYPE_CONNECT:
//                handleConnectMessage(ctx, msg);
//                break;
            case NatMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, msg);
                break;
            case NatMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            case NatMessage.TYPE_CONNECT_READY:
                handConnectReady(ctx, msg);
                break;
            default:
                log.warn("unKnown message:{}", msg.getType());
                break;
        }
    }

    private void handConnectReady(ChannelHandlerContext ctx, NatMessage msg) {
        Channel natChannel = ctx.channel();
        String clientId = natChannel.attr(Constant.NAT_CHANNEL_CLIENT_KEY).get();
        if (clientId == null) {
            //not happened
            log.error("no client bound for channel:{}", natChannel);
            ctx.close();
            return;
        }

        long seq = msg.getSerialNumber();

        NatClientImage client = clientManager.getClient(clientId);
        if (client == null) {
            log.error("now client registered for clientId:{}", clientId);
            ctx.close();
            return;
        }

        Channel userMappingChannel = client.queryUserMappingChannel(seq);
        if (userMappingChannel == null) {
            log.warn("can not find userMapping channel for request :{} client:{} when connection ready", seq, client);
            NatMessage natMessage = new NatMessage();
            natMessage.setType(NatMessage.TYPE_DISCONNECT);
            natMessage.setSerialNumber(seq);
            natChannel.writeAndFlush(natMessage);
            return;
        }
        //开始接收代理业务的数据请求
        userMappingChannel.config().setOption(ChannelOption.AUTO_READ, true);
    }

    private void handleRegister(ChannelHandlerContext ctx, NatMessage msg) {
        String clientId = msg.getExtra();
        boolean registerStatus = clientManager.registerNewClient(clientId, ctx.channel());
        if (!registerStatus) {
            log.warn("the client :{} register failed,close channel", clientId);
            ctx.channel().close();
        }
    }


    private void handleTransferMessage(ChannelHandlerContext ctx, NatMessage msg) {
        Channel natChannel = ctx.channel();
        String clientId = natChannel.attr(Constant.NAT_CHANNEL_CLIENT_KEY).get();
        if (clientId == null) {
            //not happened
            log.error("no client bound for channel:{}", natChannel);
            ctx.close();
            return;
        }

        long seq = msg.getSerialNumber();

        NatClientImage client = clientManager.getClient(clientId);
        if (client == null) {
            log.error("now client registered for clientId:{}", clientId);
            ctx.close();
            return;
        }

        Channel userMappingChannel = client.queryUserMappingChannel(seq);
        if (userMappingChannel == null) {
            log.warn("can not find userMapping channel for request :{} client:{} ,send a close message to client  endpoint", seq, client);
            NatMessage natMessage = new NatMessage();
            natMessage.setType(NatMessage.TYPE_DISCONNECT);
            natMessage.setSerialNumber(seq);
            natChannel.writeAndFlush(natMessage);
            return;
        }

        log.info("forward data from nat client:{} to user endpoint with request:{}", clientId, seq);
        byte[] data = msg.getData();
        ByteBuf buf = ctx.alloc().buffer(data.length);
        buf.writeBytes(data);
        userMappingChannel.writeAndFlush(buf);
        log.info("reply completed for clientId:{} for request:{}", clientId, seq);
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, NatMessage msg) {
        Channel natChannel = ctx.channel();
        String clientId = natChannel.attr(Constant.NAT_CHANNEL_CLIENT_KEY).get();
        if (clientId == null) {
            //not happened
            log.error("no client bound for channel:{}", natChannel);
            ctx.close();
            return;
        }

        long seq = msg.getSerialNumber();
        NatClientImage client = clientManager.getClient(clientId);
        if (client == null) {
            log.error("now client registered for clientId:{}", clientId);
            ctx.close();
            return;
        }


        Channel userMappingChannel = client.queryUserMappingChannel(seq);
        if (userMappingChannel == null) {
            log.warn("can not find userMapping channel for request :{} client:{} when recieved disconnect message", seq, client);
            return;
        }
        log.info("close user mapping due to sever endpoint closed for request:{}", seq);
        userMappingChannel.close();
    }


    private void handleHeartbeatMessage(ChannelHandlerContext ctx, NatMessage msg) {
        Channel natChannel = ctx.channel();
        String clientId = natChannel.attr(Constant.NAT_CHANNEL_CLIENT_KEY).get();
        if (clientId == null) {
            //not happened
            log.error("no client bound for channel:{}", natChannel);
            ctx.close();
            return;
        }
        log.info("receive heartbeat message from client:{}", clientId);

//        NatMessage natMessage = new NatMessage();
//        natMessage.setType(NatMessage.TYPE_HEARTBEAT);
//        ctx.channel().writeAndFlush(natMessage);
//
//
//        log.info("reply heartbeat message to client: {}", clientId);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel natChannel = ctx.channel();
        String clientId = natChannel.attr(Constant.NAT_CHANNEL_CLIENT_KEY).get();
        if (clientId == null) {
            //not happened
            log.error("no client bound for channel:{}", natChannel);
            ctx.close();
            return;
        }
        Map<Long, Channel> userMappingChannel = natChannel.attr(Constant.userMappingChannelForNatChannel).get();
        for (Channel channel : userMappingChannel.values()) {
            if (channel.isActive()) {
                channel.close();
            }
        }
        NatClientImage client = clientManager.getClient(clientId);
        Channel natChannel1 = client.getNatChannel();
        if (natChannel == natChannel1) {
            //nat channel 断开，需要关闭mapping port
            log.info("the nat channel InActive ,close mapping port ");
            client.getUserMappingServerChannel().close().get();
        }

//        log.info("the nat channel InActive ,close ");
//        clientManager.closeClient(clientId);
        super.channelInactive(ctx);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //TODO 这里应该怎么办？？？
        super.exceptionCaught(ctx, cause);
    }
}
